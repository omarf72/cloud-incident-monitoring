package com.omar.incident_monitoring.exception;

public class IncidentNotFound extends RuntimeException {

    public IncidentNotFound(Long id){
        super("Incident with id "+ id+ " not found " );
    }
    
    
}
