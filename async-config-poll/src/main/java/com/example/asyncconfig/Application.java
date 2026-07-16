package com.example.asyncconfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * async-config-poll — a small rule engine that serves /health and
 * /rules/{useCase} on port 8080 (backed by MySQL), fetches boot-blocking
 * config at startup, and then long-polls a config service in the background
 * for version changes (see {@link com.example.asyncconfig.config.ConfigWatchService}).
 */
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
