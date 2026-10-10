package com.omar.incident_monitoring.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;


import com.omar.incident_monitoring.entity.User;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;



class JwtServiceTest {

    @Test
    void generateTokenSuccessFully(){

         
        JwtService jwtService=new JwtService(
        "test-secret-key-for-jwt-unit-tests-12345");

        User user=new User();

        user.setId(1L);
        user.setEmail("test@example.com");

        String token=jwtService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(1L, jwtService.getUserIdFromToken(token));

    }

    @Test
    void rejectsModifiedToken() {

    JwtService jwtService = new JwtService(
        "test-secret-key-for-jwt-unit-tests-12345"
    );

    User user = new User();
    user.setId(1L);
    user.setEmail("test@example.com");

    String token = jwtService.generateToken(user);

    // Your turn:
    // 1. Modify one character in the token.
    int lastDot= token.lastIndexOf(".");
    String modifiedToken=token.substring(0,lastDot+1)
    +(token.charAt(lastDot+1)=='a'?'b':'a')+token.substring(lastDot+2);

    // 2. Assert that getUserIdFromToken(modifiedToken)
    //    throws a JWT parsing/validation exception.
    assertThrows(JwtException.class,
        ()-> jwtService.getUserIdFromToken(modifiedToken));

}

    @Test
    void expiredToken(){

        User user=new User();

        user.setId(1L);
        user.setEmail("test@example.com");


        String secret = "test-secret-key-for-jwt-unit-tests-12345";

        JwtService jwtService=new JwtService(
        secret);

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));


        String expiredToken=Jwts.builder().subject(user.getId().toString())
        .claim("email", user.getEmail()).issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis()-1000))
        .signWith(key).compact();

        assertThrows(ExpiredJwtException.class,() -> 
        jwtService.getUserIdFromToken(expiredToken));
    }


}
