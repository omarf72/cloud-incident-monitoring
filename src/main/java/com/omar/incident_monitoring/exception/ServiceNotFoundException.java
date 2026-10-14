package com.omar.incident_monitoring.exception;

public class ServiceNotFoundException extends RuntimeException {
    

    public  ServiceNotFoundException(Long serviceId){
        super("Service not found with id:"+ serviceId);
    }
}
