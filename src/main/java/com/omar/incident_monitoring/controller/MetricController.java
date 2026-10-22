package com.omar.incident_monitoring.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.omar.incident_monitoring.dto.MetricCreateRequest;
import com.omar.incident_monitoring.dto.MetricResponse;
import com.omar.incident_monitoring.service.MetricManager;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/applications/{applicationId}/services/{serviceId}/metrics")
public class MetricController {

    private final MetricManager metricManager;

    public MetricController(MetricManager metricManager) {
        this.metricManager = metricManager;
    }

    @PostMapping
    public MetricResponse createMetric(
            @PathVariable Long applicationId,
            @PathVariable Long serviceId,
            @Valid @RequestBody MetricCreateRequest request) {

        Long userId = getAuthenticatedUserId();

        return metricManager.createMetric(
                applicationId, serviceId, userId, request);
    }

    @GetMapping
    public List<MetricResponse> getMetrics(
            @PathVariable Long applicationId,
            @PathVariable Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        Long userId = getAuthenticatedUserId();

        return metricManager.getMetrics(
                applicationId, serviceId, userId, start, end);
    }

    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return (Long) authentication.getPrincipal();
    }
}
