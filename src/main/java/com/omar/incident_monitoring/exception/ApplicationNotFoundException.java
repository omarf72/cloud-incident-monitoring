package com.omar.incident_monitoring.exception;

public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(Long id){
        super("Application with id" + id +" was not found");
    }
    
}
