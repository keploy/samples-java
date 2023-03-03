package com.example.samplesjavajwt.config;

import com.example.samplesjavajwt.filter.JwtRequestFilter;
import com.example.samplesjavajwt.service.MyUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.samplesjavajwt.config.AuthenticationConfig;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.Filter;

@Configuration
@EnableWebSecurity
public class securityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private MyUserDetailsService myuserDetailsService ;

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
//        For Authentication
//        http.cors().csrf().disable(): disables Cross-Site Request Forgery protection and Cross origin from API
        http
                .csrf().disable().authorizeRequests()
//                allow everyone to access sign up url without authentication
                .antMatchers(AuthenticationConfig.LOGIN).permitAll()
//                Authenticate any incoming request
                .anyRequest().authenticated()

//                For Authorization
                .and().sessionManagement()
//                this disables session creation on Spring Security
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

//        call our filter "jwtRequestFilter" before the default filter
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception{
        auth.userDetailsService(myuserDetailsService);
    }

    @Override
    @Bean
//    AuthenticationManager without overridden is not used, it's deprecated
    public AuthenticationManager authenticationManagerBean() throws Exception{
        return super.authenticationManagerBean();
    }

    @Bean
    public PasswordEncoder passwordEncoder (){
        return NoOpPasswordEncoder.getInstance();
    }
}
