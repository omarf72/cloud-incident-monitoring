package com.omar.incident_monitoring.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.omar.incident_monitoring.dto.AuthResponse;
import com.omar.incident_monitoring.dto.LoginRequest;
import com.omar.incident_monitoring.dto.RegisterRequest;
import com.omar.incident_monitoring.service.AuthService;



@RestController 
@RequestMapping("/api/auth")
public class AuthController {
    
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    // http://localhost:8080/api/auth/register

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request){
       return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request){
        return authService.login(request);
    }

    
}
