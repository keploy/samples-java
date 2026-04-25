package com.keploy.hibernatecachetour;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point. EnableCaching switches on Spring's cache abstraction so the
 * Hibernate L2/query cache wired in application.properties is actually used
 * end-to-end.
 */
@SpringBootApplication
@EnableCaching
public class HibernateCacheTourApplication {
    public static void main(String[] args) {
        SpringApplication.run(HibernateCacheTourApplication.class, args);
    }
}
