package com.omar.incident_monitoring.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.omar.incident_monitoring.dto.ApplicationCreateRequest;
import com.omar.incident_monitoring.dto.ApplicationHealthResponse;
import com.omar.incident_monitoring.dto.ApplicationResponse;
import com.omar.incident_monitoring.service.ApplicationHealthManager;
import com.omar.incident_monitoring.service.ApplicationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationHealthManager applicationHealthManager;

    public ApplicationController(
            ApplicationService applicationService,
            ApplicationHealthManager applicationHealthManager) {
        this.applicationService = applicationService;
        this.applicationHealthManager = applicationHealthManager;
    }

    @GetMapping
    public List<ApplicationResponse> getApplications() {
        return applicationService.getAllApplications();
    }

    @GetMapping("/{id}")
    public ApplicationResponse getApplicationById(@PathVariable Long id) {
        return applicationService.getApplicationById(id);
    }

    @GetMapping("/{applicationId}/health")
    public ApplicationHealthResponse getApplicationHealth(
            @PathVariable Long applicationId) {
        return applicationHealthManager.getApplicationHealth(applicationId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse createApplication(
            @Valid @RequestBody ApplicationCreateRequest request) {
        return applicationService.createApplication(request);
    }
}
