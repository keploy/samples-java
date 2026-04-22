package com.tricentisdemo.sap.customer360;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Minimal context-load smoke test. Verifies the Spring wiring is correct —
 * RestTemplate, interceptors, executor, Resilience4j decorators all bind.
 *
 * <p>{@link AutoConfigureTestDatabase} replaces the configured Postgres
 * with an in-memory H2 at test time so this test can run in environments
 * without a live DB. Flyway is disabled for the same reason (migrations
 * are Postgres-specific); Hibernate creates the schema from entities.
 *
 * <p>The full integration test suite is intentionally out of scope for
 * this reference service: the regression layer is provided by Keploy
 * mocks recorded from real SAP traffic. That is the whole point of the
 * demo.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
    "sap.api.base-url=https://example.invalid",
    "sap.api.key=test-key",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class Customer360ApplicationTests {

    @Test
    void contextLoads() {
        // if Spring wires everything, we're good
    }
}
