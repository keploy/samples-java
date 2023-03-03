package io.nkcm.jwt.jwtAuth.Exception;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        
        if (authException instanceof BadCredentialsException || authException instanceof UsernameNotFoundException) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized User");
        } else if (authException instanceof LockedException) {
            response.sendError(HttpStatus.LOCKED.value(),"Account locked");
        } else {
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(),"Internal server error");
        }

    }
}
