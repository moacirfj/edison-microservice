package de.otto.edison.logging.ui;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.springframework.boot.SpringApplication.run;

@SpringBootApplication(scanBasePackages = "de.otto.edison")
public class LoggingTestServer {

    public static void main(String[] args) {
        run(LoggingTestServer.class, args);
    }

}
