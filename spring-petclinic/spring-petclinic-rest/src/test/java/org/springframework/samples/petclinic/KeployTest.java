package org.springframework.samples.petclinic;

import java.io.IOException;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.keploy.Keploy;

public class KeployTest {
@Test
@Order(Integer.MAX_VALUE)
public void testKeploy() throws IOException, InterruptedException {
    String jarPath = "target/springbootapp-0.0.1-SNAPSHOT.jar";
    Keploy.runTests(jarPath);
}
}
