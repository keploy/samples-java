package io.nkcm.jwt.jwtAuth.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import io.nkcm.jwt.jwtAuth.Exception.JwtAuthEntryPoint;
import io.nkcm.jwt.jwtAuth.services.CustomUserDetailService;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private CustomUserDetailService customUserDetailService;

    @Autowired
    private JwtValidatingFilter jwtFilter;

    @Autowired
    private JwtAuthEntryPoint jwtAuthEntryPoint;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // what kind of Auth you want to implement
        auth.userDetailsService(customUserDetailService);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // filtering the urls for authorization
        http.cors().and().csrf().disable()
            .authorizeRequests()
            .antMatchers("/publicToken").permitAll()
            .anyRequest().authenticated()
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .exceptionHandling().authenticationEntryPoint(jwtAuthEntryPoint);
      
        //intercept the request evertime a request is made to the server
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
      return authConfig.getAuthenticationManager();
    }
  
    @Bean
    public PasswordEncoder passwordEncoder() {
        // for production level code, use hashing algorithm like bcrypt or PBKDF2
      return NoOpPasswordEncoder.getInstance(); 
    }
    
    
}
