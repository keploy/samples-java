package com.keploy.sapdemo.customer360;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Minimal context-load smoke test. Verifies the Spring wiring is correct —
 * RestTemplate, interceptors, executor, Resilience4j decorators all bind.
 *
 * <p><b>This test expects a live Postgres instance on localhost:5432</b>
 * (see docker-compose.yml). That is deliberate: the sample exists to
 * validate that Keploy correctly captures real Postgres traffic —
 * including the Flyway bootstrap queries — so swapping the datasource
 * for H2 would hide exactly the wire-protocol behaviour we regress.
 * Bring the compose stack up (`docker compose up -d postgres`) before
 * running tests, or run via the CI pipeline where Postgres is provisioned.
 *
 * <p>The full integration test suite is intentionally out of scope for
 * this reference service: the regression layer is provided by Keploy
 * mocks recorded from real SAP traffic.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "sap.api.base-url=https://example.invalid",
    "sap.api.key=test-key"
})
class Customer360ApplicationTests {

    @Test
    void contextLoads() {
        // if Spring wires everything, we're good
    }
}
