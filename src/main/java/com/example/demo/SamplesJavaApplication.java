package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//@SpringBootApplication(scanBasePackages = {"com.example.demo","io.keploy"})
@SpringBootApplication
public class SamplesJavaApplication {

	public static void main(String[] args) {
//		KeployInstance ki = KeployInstance.getInstance();
		SpringApplication.run(SamplesJavaApplication.class, args);
	}

}

