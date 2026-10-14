package com.omar.incident_monitoring.controller;

import org.springframework.web.bind.annotation.*;
import com.omar.incident_monitoring.service.ApplicationService;
import com.omar.incident_monitoring.dto.ApplicationResponse;
import com.omar.incident_monitoring.dto.ApplicationCreateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

import java.util.List;

/*
http://localhost:8080/api/applications
/* */

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public List<ApplicationResponse> getApplications() {
        return applicationService.getAllApplications();
    }

    @GetMapping("/{id}")
    public ApplicationResponse getApplicationById(@PathVariable Long id) {
        return applicationService.getApplicationById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse createApplication(
            @Valid @RequestBody ApplicationCreateRequest request) {
        return applicationService.createApplication(request);
    }

}
