package com.omar.incident_monitoring.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.omar.incident_monitoring.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;

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


}
