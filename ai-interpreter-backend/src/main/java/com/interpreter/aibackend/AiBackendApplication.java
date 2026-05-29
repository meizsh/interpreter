package com.interpreter.aibackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@EnableAsync
public class AiBackendApplication {
    private static final Logger logger = LoggerFactory.getLogger(AiBackendApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AiBackendApplication.class, args);
        logger.info("AI interpreter backend started on http://localhost:8080");
    }
}
