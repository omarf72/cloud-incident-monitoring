package com.omar.incident_monitoring.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.omar.incident_monitoring.dto.IncidentCreateRequest;
import com.omar.incident_monitoring.dto.IncidentResponse;
import com.omar.incident_monitoring.service.IncidentManager;

import jakarta.validation.Valid;

@RestController 
@RequestMapping("/api/applications/{applicationId}/incidents")
public class IncidentController {

    private final IncidentManager incidentManager;

    public IncidentController(IncidentManager incidentManager){
        this.incidentManager=incidentManager;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse createIncident(
        @PathVariable  Long applicationId,
        @Valid @RequestBody IncidentCreateRequest request
    ){
        return incidentManager.createIncident(applicationId, request);
    }
    
}
