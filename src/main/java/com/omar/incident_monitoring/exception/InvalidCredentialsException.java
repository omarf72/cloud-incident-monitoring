package com.omar.incident_monitoring.exception;

public class InvalidCredentialsException extends RuntimeException
 {

    public InvalidCredentialsException(){
        super("Invalid email or password");
    }
    
}
