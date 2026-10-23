package com.omar.incident_monitoring.service;

import java.util.List;

import com.omar.incident_monitoring.dto.MetricThresholdCreateRequest;
import com.omar.incident_monitoring.dto.MetricThresholdResponse;
import com.omar.incident_monitoring.entity.Application;
import com.omar.incident_monitoring.entity.MetricThreshold;
import com.omar.incident_monitoring.entity.Metric.MetricType;
import com.omar.incident_monitoring.entity.Service;
import com.omar.incident_monitoring.exception.ApplicationNotFoundException;
import com.omar.incident_monitoring.exception.ServiceNotFoundException;
import com.omar.incident_monitoring.repository.ApplicationRepository;
import com.omar.incident_monitoring.repository.MetricThresholdRepository;
import com.omar.incident_monitoring.repository.ServiceRepository;

@org.springframework.stereotype.Service

public class MetricThresholdManager {

    private final ApplicationRepository applicationRepository;
    private final ServiceRepository serviceRepository;
    private final MetricThresholdRepository metricThresholdRepository;

    public MetricThresholdManager(
            ApplicationRepository applicationRepository,
            ServiceRepository serviceRepository,
            MetricThresholdRepository metricThresholdRepository) {
        this.applicationRepository = applicationRepository;
        this.serviceRepository = serviceRepository;
        this.metricThresholdRepository = metricThresholdRepository;
    }

    public MetricThresholdResponse createThreshold(
            Long applicationId,
            Long serviceId,
            Long userId,
            MetricThresholdCreateRequest request) {

        // Verify the application belongs to the authenticated user.
        applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() ->
                        new ApplicationNotFoundException(applicationId));

        // Verify the service belongs to that application.
        Service service = serviceRepository
                .findByIdAndApplicationId(serviceId, applicationId)
                .orElseThrow(() ->
                        new ServiceNotFoundException(serviceId));

        validateThresholdValue(
                request.metricType(),
                request.thresholdValue());

        MetricThreshold threshold = new MetricThreshold();
        threshold.setService(service);
        threshold.setMetricType(request.metricType());
        threshold.setOperator(request.operator());
        threshold.setThresholdValue(request.thresholdValue());

        MetricThreshold saved =
                metricThresholdRepository.save(threshold);

        return toResponse(saved);
    }

    public List<MetricThresholdResponse> getThresholds(
            Long applicationId,
            Long serviceId,
            Long userId) {

        applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() ->
                        new ApplicationNotFoundException(applicationId));

        serviceRepository.findByIdAndApplicationId(serviceId, applicationId)
                .orElseThrow(() ->
                        new ServiceNotFoundException(serviceId));

        return metricThresholdRepository.findByService_Id(serviceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void validateThresholdValue(
            MetricType metricType,
            double value) {

        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(
                    "Threshold value must be finite and non-negative");
        }

        switch (metricType) {
            case CPU_USAGE_PERCENT,
                 MEMORY_USAGE_PERCENT,
                 DISK_USAGE_PERCENT,
                 ERROR_RATE_PERCENT,
                 SUCCESS_RATE_PERCENT,
                 DB_CONNECTION_POOL_USAGE_PERCENT,
                 DB_CACHE_HIT_RATE_PERCENT,
                 AVAILABILITY_PERCENT -> {
                if (value > 100) {
                    throw new IllegalArgumentException(
                            "Percentage thresholds cannot exceed 100");
                }
            }

            case HEALTH_CHECK_STATUS ->
                    throw new IllegalArgumentException(
                            "HEALTH_CHECK_STATUS cannot use a numeric threshold");

            default -> {
                // Other metric types use non-negative numeric thresholds.
            }
        }
    }

    private MetricThresholdResponse toResponse(
            MetricThreshold threshold) {

        return new MetricThresholdResponse(
                threshold.getId(),
                threshold.getService().getId(),
                threshold.getMetricType(),
                threshold.getOperator(),
                threshold.getThresholdValue(),
                threshold.isEnabled(),
                threshold.getCreatedAt());
    }
}
