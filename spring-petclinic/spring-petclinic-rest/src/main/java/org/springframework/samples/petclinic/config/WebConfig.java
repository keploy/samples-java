package org.springframework.samples.petclinic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.google.cloud.sqlcommenter.interceptors.SpringSQLCommenterInterceptor;

@EnableWebMvc
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public SpringSQLCommenterInterceptor sqlInterceptor() {
         return new SpringSQLCommenterInterceptor();
    }
 
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        System.out.println("adding the sql interceptor from SpringSQLCommenterInterceptor");
        registry.addInterceptor(sqlInterceptor());
    }
}
