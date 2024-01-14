package org.springframework.samples.petclinic.config;

import com.google.cloud.sqlcommenter.schibernate.SCHibernate;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Properties;
// ... other imports

@Configuration
@EnableTransactionManagement
public class JPAConfig {

    // ... other beans like entityManagerFactory

    private Properties additionalProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.session_factory.statement_inspector", SCHibernate.class.getName());
        return properties;
    }
}
