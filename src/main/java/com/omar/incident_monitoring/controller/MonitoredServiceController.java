package com.omar.incident_monitoring.controller;

import com.omar.incident_monitoring.dto.MonitoredServiceResponse;
import com.omar.incident_monitoring.dto.ServiceCreateRequest;
import com.omar.incident_monitoring.entity.Service;
import com.omar.incident_monitoring.service.MonitoredServiceManager;

import jakarta.validation.Valid;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/applications/{applicationId}/services")
public class MonitoredServiceController {

    private final MonitoredServiceManager monitoredServiceManager;

    public MonitoredServiceController(
            MonitoredServiceManager monitoredServiceManager) {
        this.monitoredServiceManager = monitoredServiceManager;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MonitoredServiceResponse createService(
            @PathVariable Long applicationId,
            @Valid @RequestBody ServiceCreateRequest request) {
        return monitoredServiceManager.createService(applicationId, request);
    }

    @GetMapping
    public List<MonitoredServiceResponse> getServices(@PathVariable Long applicationId) {
        return monitoredServiceManager.getServicesForApplication(applicationId);
    }

    @PutMapping("/{serviceId}")
    public MonitoredServiceResponse updateServices(
        @PathVariable  Long applicationId,
        @PathVariable Long serviceId,
        @Valid @RequestBody ServiceCreateRequest request
    ){
        return monitoredServiceManager.updateService(applicationId, serviceId, request);
    }

    
}
