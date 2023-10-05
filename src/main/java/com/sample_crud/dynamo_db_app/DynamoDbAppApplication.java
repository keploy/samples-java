package com.sample_crud.dynamo_db_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.context.annotation.Import;
// import io.keploy.servlet.KeployMiddleware;

@SpringBootApplication
// @Import(KeployMiddleware.class)
public class DynamoDbAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(DynamoDbAppApplication.class, args);
	}

}
