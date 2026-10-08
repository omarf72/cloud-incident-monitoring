package com.omar.incident_monitoring.exception;

public class EmailAlreadyExistsException extends RuntimeException{
    
    public EmailAlreadyExistsException(String email){
        super("User with email "+email + " already exists");
    }
}
