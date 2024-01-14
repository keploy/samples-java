package org.springframework.samples.petclinic.config;

import java.util.Map;

import com.google.cloud.sqlcommenter.schibernate.SCHibernate;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HibernateConfig {

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return new HibernatePropertiesCustomizer() {
            @Override
            public void customize(Map<String, Object> hibernateProperties) {
                hibernateProperties.put("hibernate.session_factory.statement_inspector", new SCHibernate());
            }
        };
    }
}