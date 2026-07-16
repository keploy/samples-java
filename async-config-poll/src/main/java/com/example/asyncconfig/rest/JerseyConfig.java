package com.example.asyncconfig.rest;

import com.example.asyncconfig.rules.HealthResource;
import com.example.asyncconfig.rules.RulesResource;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

/**
 * Registers the JAX-RS resources with Jersey (the app's REST layer). Both
 * /health and /rules are served here.
 */
@Component
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig() {
        register(HealthResource.class);
        register(RulesResource.class);
    }
}
