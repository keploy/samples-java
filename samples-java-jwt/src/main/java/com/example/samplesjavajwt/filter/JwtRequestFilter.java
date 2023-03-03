package com.example.samplesjavajwt.filter;

import com.example.samplesjavajwt.config.AuthenticationConfig;
import com.example.samplesjavajwt.service.MyUserDetailsService;
import com.example.samplesjavajwt.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.example.samplesjavajwt.config.AuthenticationConfig;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private MyUserDetailsService myUserDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
//    identify the given token of the captured incoming request
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
//        Capture the request
        String header = request.getHeader(AuthenticationConfig.HEADER_STRING);
        String username = null;
        String jwt = null;

//        Is there any token present?
        if (header != null && header.startsWith(AuthenticationConfig.TOKEN_PREFIX)) {
            jwt = header.substring(7); //length of TOKEN_PREFIX
            username = jwtUtil.extractUsername(jwt);
        }

        if(username != null && SecurityContextHolder.getContext().getAuthentication() == null)
        {
//            if there is a token and this token wasn't made before in security
            UserDetails userDetails = this.myUserDetailsService.loadUserByUsername(username);
//            check if the token is typical to what we received and not expired
            if (jwtUtil.validateToken(jwt, userDetails))
            {
//                create the default token
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

            }
        }
        chain.doFilter(request, response);
    }
}
