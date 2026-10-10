package com.omar.incident_monitoring.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.omar.incident_monitoring.dto.AuthResponse;
import com.omar.incident_monitoring.dto.LoginRequest;
import com.omar.incident_monitoring.dto.RegisterRequest;
import com.omar.incident_monitoring.entity.User;
import com.omar.incident_monitoring.repository.UserRepository;

import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.omar.incident_monitoring.exception.EmailAlreadyExistsException;
import com.omar.incident_monitoring.exception.InvalidCredentialsException;

import static org.mockito.Mockito.never;
import com.omar.incident_monitoring.dto.LoginRequest;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerSuccessfully() {

        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");

        User savedUser = new User();

        savedUser.setId(1L);
        savedUser.setEmail("test@example.com");
        savedUser.setCreatedAt(LocalDateTime.now());

        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);

        AuthResponse response = authService.register(request);

        assertEquals(1L, response.getId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(savedUser.getCreatedAt(), response.getCreatedAt());

        verify(passwordEncoder).encode("password123");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();

        assertEquals("encoded-password", capturedUser.getPasswordHash());
    }

    @Test
    void registerRejectsDuplicateEmail() {

        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        User existingUser = new User();

        existingUser.setId(1L);
        existingUser.setEmail("existing@example.com");

        when(userRepository.findByEmail("existing@example.com"))
                .thenReturn(Optional.of(existingUser));

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(request));

        verify(passwordEncoder, never()).encode("password123");
        verify(userRepository, never()).save(any(User.class));

    }

    @Test
    void loginSuccessFully() {
        LoginRequest request = new LoginRequest();

        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = new User();

        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPasswordHash("encoded-password");
        user.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password123",
                "encoded-password")).thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("fake-jwt-token");

        AuthResponse response = authService.login(request);

        assertEquals(1L, response.getId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(user.getCreatedAt(), response.getCreatedAt());
        assertEquals("fake-jwt-token", response.getToken());
    }

    @Test
    void loginFails() {
        LoginRequest request = new LoginRequest();

        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = new User();

        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPasswordHash("encoded-password");
        user.setCreatedAt(LocalDateTime.now());

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password123",
        "encoded-password"))
            .thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
            ()->authService.login(request));
        
        verify(jwtService,never()).generateToken(user);

    }
    @Test
    void loginFailsWhenUserDoesNotExist() {
    LoginRequest request = new LoginRequest();
    request.setEmail("missing@example.com");
    request.setPassword("password123");

    when(userRepository.findByEmail("missing@example.com"))
            .thenReturn(Optional.empty());

    // Your turn:
    // 1. Assert that InvalidCredentialsException is thrown.
    assertThrows(InvalidCredentialsException.class,
        ()-> authService.login(request)
    );
    // 2. Verify passwordEncoder.matches() is never called.
    verify(passwordEncoder,never()).matches(anyString(),
     anyString());

    // 3. Verify jwtService.generateToken() is never called.
    verify(jwtService,never()).generateToken(any(User.class));

}

}
