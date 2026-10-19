package com.omar.incident_monitoring.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.omar.incident_monitoring.dto.IncidentCreateRequest;
import com.omar.incident_monitoring.dto.IncidentPageResponse;
import com.omar.incident_monitoring.dto.IncidentResponse;
import com.omar.incident_monitoring.dto.IncidentUpdateRequest;
import com.omar.incident_monitoring.entity.Incident;
import com.omar.incident_monitoring.service.IncidentManager;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/applications/{applicationId}/incidents")
public class IncidentController {

    private final IncidentManager incidentManager;

    public IncidentController(IncidentManager incidentManager) {
        this.incidentManager = incidentManager;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse createIncident(
            @PathVariable Long applicationId,
            @Valid @RequestBody IncidentCreateRequest request) {
        return incidentManager.createIncident(applicationId, request);
    }

    @GetMapping
    public IncidentPageResponse getIncidents(
            @PathVariable Long applicationId,
            @RequestParam(required = false) Incident.IncidentStatus status,
            @RequestParam(required = false) Incident.IncidentSeverity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be 0 or greater and size must be between 1 and 100");
        }

        return incidentManager.getIncidents(
                applicationId, status, severity, page, size);
    }

    @PatchMapping("/{incidentId}")
    public IncidentResponse updateIncident(
            @PathVariable Long applicationId,
            @PathVariable Long incidentId,
            @Valid @RequestBody IncidentUpdateRequest request) {
        return incidentManager.updateIncident(
                incidentId, applicationId, request);
    }

}
