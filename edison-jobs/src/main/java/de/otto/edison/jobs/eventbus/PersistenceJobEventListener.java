package de.otto.edison.jobs.eventbus;

import de.otto.edison.jobs.domain.JobInfo;
import de.otto.edison.jobs.eventbus.events.MessageEvent;
import de.otto.edison.jobs.eventbus.events.StateChangeEvent;
import de.otto.edison.jobs.repository.JobRepository;
import de.otto.edison.status.domain.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Clock;
import java.util.Optional;
import java.util.function.Consumer;

import static de.otto.edison.jobs.domain.JobInfo.newJobInfo;

public class PersistenceJobEventListener implements JobEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(PersistenceJobEventListener.class);

    private final JobRepository jobRepository;
    private final Clock clock;
    private final SystemInfo systemInfo;

    public PersistenceJobEventListener(final JobRepository jobRepository, final Clock clock,
                                       final SystemInfo systemInfo) {
        this.jobRepository = jobRepository;
        this.clock = clock;
        this.systemInfo = systemInfo;
    }

    @Override
    public void consumeStateChange(final StateChangeEvent event) {
        switch (event.getState()) {
            case START:
                jobRepository.createOrUpdate(newJobInfo(event.getJobUri(), event.getJobType(), clock,
                        systemInfo.getHostname()));
                break;

            case KEEP_ALIVE:
                updateJobIfPresent(event.getJobUri(), JobInfo::ping);
                break;

            case RESTART:
                updateJobIfPresent(event.getJobUri(), JobInfo::restart);
                break;

            case DEAD:
                updateJobIfPresent(event.getJobUri(), JobInfo::dead);
                break;

            case STOP:
                updateJobIfPresent(event.getJobUri(), JobInfo::stop);
                break;
        }
    }

    @Override
    public void consumeMessage(final MessageEvent messageEvent) {
        MessageEvent.Level level = messageEvent.getLevel();
        switch (level) {
            case ERROR:
                updateJobIfPresent(messageEvent.getJobUri(), jobInfo -> jobInfo.error(messageEvent.getMessage()));
                break;
            case WARN:
                updateJobIfPresent(messageEvent.getJobUri(), jobInfo -> jobInfo.warn(messageEvent.getMessage()));
                break;
            case INFO:
                updateJobIfPresent(messageEvent.getJobUri(), jobInfo -> jobInfo.info(messageEvent.getMessage()));
                break;
        }
    }

    private void updateJobIfPresent(final URI jobUri, final Consumer<JobInfo> consumer) {
        final Optional<JobInfo> jobInfo = jobRepository.findOne(jobUri);
        if (jobInfo.isPresent()) {
            consumer.accept(jobInfo.get());
            jobRepository.createOrUpdate(jobInfo.get());
        } else {
            LOG.error("job '{}' in inconsistent state. Should be present here", jobUri);
        }
    }
}