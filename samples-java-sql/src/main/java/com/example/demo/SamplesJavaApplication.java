package com.example.demo;

import io.keploy.servlet.KeployMiddleware;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(KeployMiddleware.class)
public class SamplesJavaApplication {
    public static void main(String[] args) {
        SpringApplication.run(SamplesJavaApplication.class, args);
    }
}

