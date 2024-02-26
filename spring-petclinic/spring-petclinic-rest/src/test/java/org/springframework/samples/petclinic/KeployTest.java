package org.springframework.samples.petclinic;

import java.io.IOException;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.keploy.Keploy;

public class KeployTest {
@Test
@Order(Integer.MAX_VALUE)
public void testKeploy() throws IOException, InterruptedException {
    String jarPath = "target/spring-petclinic-rest-3.0.2.jar";
    Keploy.runTests(jarPath);
}
}
