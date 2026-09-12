package io.bootify.copilot_middleware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class CopilotMiddlewareApplication {

    public static void main(final String[] args) {
        SpringApplication.run(CopilotMiddlewareApplication.class, args);
    }

}
