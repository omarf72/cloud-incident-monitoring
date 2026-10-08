package com.omar.incident_monitoring.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.omar.incident_monitoring.dto.AuthResponse;
import com.omar.incident_monitoring.dto.LoginRequest;
import com.omar.incident_monitoring.dto.RegisterRequest;
import com.omar.incident_monitoring.entity.User;
import com.omar.incident_monitoring.repository.UserRepository;
import com.omar.incident_monitoring.exception.EmailAlreadyExistsException;
import com.omar.incident_monitoring.exception.InvalidCredentialsException;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,PasswordEncoder passwordEncoder,JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder=passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request){
       if (userRepository.findByEmail(request.getEmail()).isPresent()){
            throw new EmailAlreadyExistsException(request.getEmail());
       }
       User user=new User();
       user.setEmail(request.getEmail());
       user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
       user.setCreatedAt(LocalDateTime.now());

       User savedUser= userRepository.save(user);

       AuthResponse response=new AuthResponse();

       response.setId(savedUser.getId());
       response.setEmail(savedUser.getEmail());
       response.setCreatedAt(savedUser.getCreatedAt());

       return response;

    }

    public AuthResponse login(LoginRequest request){

        User user=userRepository.findByEmail(request.getEmail())
        .orElseThrow(()-> new InvalidCredentialsException());

        if(!passwordEncoder.matches(request.getPassword(),user.getPasswordHash())){
            throw new InvalidCredentialsException();
        }
        
        String token=jwtService.generateToken(user);

        AuthResponse response=new AuthResponse();

        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setCreatedAt(user.getCreatedAt());
        response.setToken(token);

        return response;
    }

}
