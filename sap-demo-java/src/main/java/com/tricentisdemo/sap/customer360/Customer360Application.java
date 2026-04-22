package com.tricentisdemo.sap.customer360;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the SAP Customer 360 aggregator service.
 *
 * This service sits between downstream consumers (CRM, partner portals,
 * analytics pipelines) and SAP S/4HANA's Business Partner OData APIs. A single
 * inbound request for a "customer 360 view" triggers parallel fan-out calls to
 * three SAP endpoints — the partner master record, associated addresses, and
 * assigned roles — aggregated into a flat response.
 *
 * In a typical RISE with SAP landscape this would run as a BTP extension
 * (Cloud Foundry or Kyma/Kubernetes). It's the kind of service that Tricentis
 * LiveCompare flags as "impacted by migration" and that teams must regression-
 * test end-to-end after every S/4HANA quarterly update.
 */
@SpringBootApplication
@EnableAsync
@EnableCaching
@OpenAPIDefinition(
    info = @Info(
        title = "SAP Customer 360 Service",
        version = "1.0.0",
        description = "Aggregates SAP Business Partner master data, addresses, and roles into a unified view.",
        contact = @Contact(name = "Integration Platform Team", email = "integration@example.com"),
        license = @License(name = "Internal — Reference Implementation")
    ),
    servers = {
        @Server(url = "/", description = "In-cluster / local")
    }
)
public class Customer360Application {

    public static void main(String[] args) {
        SpringApplication.run(Customer360Application.class, args);
    }
}
