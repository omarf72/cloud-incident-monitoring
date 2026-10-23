package com.omar.incident_monitoring.controller;

import com.omar.incident_monitoring.dto.MetricThresholdCreateRequest;
import com.omar.incident_monitoring.dto.MetricThresholdResponse;
import com.omar.incident_monitoring.service.MetricThresholdManager;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications/{applicationId}/services/{serviceId}/thresholds")
public class MetricThresholdController {

    private final MetricThresholdManager metricThresholdManager;

    public MetricThresholdController(
            MetricThresholdManager metricThresholdManager) {
        this.metricThresholdManager = metricThresholdManager;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MetricThresholdResponse createThreshold(
            @PathVariable Long applicationId,
            @PathVariable Long serviceId,
            @Valid @RequestBody MetricThresholdCreateRequest request) {

        Long userId = getAuthenticatedUserId();

        return metricThresholdManager.createThreshold(
                applicationId,
                serviceId,
                userId,
                request);
    }

    @GetMapping
    public List<MetricThresholdResponse> getThresholds(
            @PathVariable Long applicationId,
            @PathVariable Long serviceId) {

        Long userId = getAuthenticatedUserId();

        return metricThresholdManager.getThresholds(
                applicationId,
                serviceId,
                userId);
    }

    private Long getAuthenticatedUserId() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}
