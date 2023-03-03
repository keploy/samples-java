package io.nkcm.jwt.jwtAuth.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.nkcm.jwt.jwtAuth.jwtUtil.JwtUtil;
import io.nkcm.jwt.jwtAuth.services.CustomUserDetailService;

@Component
public class JwtValidatingFilter extends OncePerRequestFilter{

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailService customUserDetailService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // receive JWT and validate it
        String tokenHeader = request.getHeader("Authorization");
        String username=null;
        String jwtToken=null;
        
        if (tokenHeader != null && tokenHeader.startsWith("Bearer ")){
            jwtToken=tokenHeader.substring(7);
            
            try{
                
                username = this.jwtUtil.extractUsername(jwtToken);

            }catch (Exception e){
                e.printStackTrace();
            }

            UserDetails userDetail = this.customUserDetailService.loadUserByUsername(username);

            //security
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null){
                
                // token is beging authenticated
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetail, null, userDetail.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }else{
                System.out.println("Token couldn't get authenticated, sorry!");
            }
    
        }
        // filtering
        filterChain.doFilter(request, response);
    }

}
