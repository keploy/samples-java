package com.example.samplesjavajwt;

import io.keploy.servlet.KeployMiddleware;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(KeployMiddleware.class)

public class SamplesJavaJwtApplication {

	public static void main(String[] args) {
		SpringApplication.run(SamplesJavaJwtApplication.class, args);
	}

}
