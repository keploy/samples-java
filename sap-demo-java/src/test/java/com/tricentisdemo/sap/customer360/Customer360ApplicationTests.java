package com.tricentisdemo.sap.customer360;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Minimal context-load smoke test. Verifies the Spring wiring is correct —
 * RestTemplate, interceptors, executor, Resilience4j decorators all bind.
 *
 * <p>The full integration test suite is intentionally out of scope for this
 * reference service: the regression layer is provided by Keploy mocks
 * recorded from real SAP traffic. That is the whole point of the demo.
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
