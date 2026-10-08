package com.omar.incident_monitoring.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.omar.incident_monitoring.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import com.omar.incident_monitoring.exception.InvalidCredentialsException;

@RestControllerAdvice 
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleApplicationNotFound(
        ApplicationNotFoundException exception,HttpServletRequest request){

            ErrorResponse errorResponse=new ErrorResponse();

            errorResponse.setStatus(HttpStatus.NOT_FOUND.value());
            errorResponse.setError("Not Found");
            errorResponse.setMessage(exception.getMessage());
            errorResponse.setTimestamp(LocalDateTime.now());
            errorResponse.setPath(request.getRequestURI());

            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);

    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> emailAlreadyExists(
        EmailAlreadyExistsException exception,HttpServletRequest request){
            ErrorResponse errorResponse=new ErrorResponse();

            errorResponse.setStatus(HttpStatus.CONFLICT.value());
            errorResponse.setError("Conflict");
            errorResponse.setMessage(exception.getMessage());
            errorResponse.setTimestamp(LocalDateTime.now());
            errorResponse.setPath(request.getRequestURI());
            

            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorResponse);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> invalidCredentials(
    InvalidCredentialsException exception, HttpServletRequest request){
        
        ErrorResponse errorResponse =new ErrorResponse();

        errorResponse.setStatus((HttpStatus.UNAUTHORIZED.value()));
        errorResponse.setError("Unauthorized");
        errorResponse.setMessage(exception.getMessage());
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setPath(request.getRequestURI());

        return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body(errorResponse);

    }



}
