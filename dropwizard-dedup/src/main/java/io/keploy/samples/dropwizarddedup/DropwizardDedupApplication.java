package io.keploy.samples.dropwizarddedup;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.keploy.samples.dropwizarddedup.core.CatalogService;
import io.keploy.samples.dropwizarddedup.errors.ApiExceptionMapper;
import io.keploy.samples.dropwizarddedup.health.ApplicationHealthCheck;
import io.keploy.samples.dropwizarddedup.resources.InventoryResource;
import io.keploy.samples.dropwizarddedup.resources.OrderResource;
import io.keploy.samples.dropwizarddedup.resources.PlatformResource;

public class DropwizardDedupApplication extends Application<DropwizardDedupConfiguration> {

    public static void main(String[] args) throws Exception {
        new DropwizardDedupApplication().run(args);
    }

    @Override
    public String getName() {
        return "dropwizard-dedup";
    }

    @Override
    public void initialize(Bootstrap<DropwizardDedupConfiguration> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor(false)
        ));
    }

    @Override
    public void run(DropwizardDedupConfiguration configuration, Environment environment) {
        CatalogService catalogService = new CatalogService();
        environment.jersey().register(new InventoryResource(catalogService));
        environment.jersey().register(new OrderResource(catalogService));
        environment.jersey().register(new PlatformResource());
        environment.jersey().register(new ApiExceptionMapper());
        environment.healthChecks().register("application", new ApplicationHealthCheck());
    }
}
