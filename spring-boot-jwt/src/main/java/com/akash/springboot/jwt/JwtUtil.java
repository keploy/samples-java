package com.akash.springboot.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;

import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET_KEY = "secret_key";

    public String generateToken(String username) throws UnsupportedEncodingException {
        return Jwts.builder()
                .setIssuer("spring-boot-jwt")
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day expiration
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token);
            System.err.println("Valid");
            return true;
        } catch (SignatureException e) {
            System.err.println("Signature Exception");
            return false;
        } catch (ExpiredJwtException e) {
            System.err.println("ExpiredJwtException");
            return false;
        } catch (Exception e) {
            System.err.println("Other Exception");
            return false;
        }
    }
}