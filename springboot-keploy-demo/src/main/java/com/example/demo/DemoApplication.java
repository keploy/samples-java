package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Spring Boot + Keploy integration demo application.
 * This starts the embedded Tomcat server and initializes the Spring context.
 */
@SpringBootApplication
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        System.out.println("\n--- Spring Boot + Keploy Demo Running ---");
        System.out.println("Try these endpoints:");
        System.out.println("POST   http://localhost:8080/users");
        System.out.println("GET    http://localhost:8080/users/{id}\n");
    }
}