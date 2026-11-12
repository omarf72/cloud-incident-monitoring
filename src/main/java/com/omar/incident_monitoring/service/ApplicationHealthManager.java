package com.omar.incident_monitoring.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.omar.incident_monitoring.dto.ApplicationHealthResponse;
import com.omar.incident_monitoring.dto.ServiceHealthResponse;
//import com.omar.incident_monitoring.entity.Service;
import com.omar.incident_monitoring.repository.ApplicationRepository;
import com.omar.incident_monitoring.repository.ServiceRepository;

@Service
public class ApplicationHealthManager {

    private final ApplicationRepository applicationRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceHealthManager serviceHealthManager;

    public ApplicationHealthManager(
            ApplicationRepository applicationRepository,
            ServiceRepository serviceRepository,
            ServiceHealthManager serviceHealthManager) {
        this.applicationRepository = applicationRepository;
        this.serviceRepository = serviceRepository;
        this.serviceHealthManager = serviceHealthManager;
    }

    public ApplicationHealthResponse getApplicationHealth(Long applicationId) {
        Long userId = getAuthenticatedUserId();

        applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application not found"));

        List<com.omar.incident_monitoring.entity.Service> monitoredServices = serviceRepository.findAllByApplicationId(applicationId);

        List<ServiceHealthResponse> serviceHealthResults = monitoredServices.stream()
                .map(service -> serviceHealthManager.getServiceHealth(
                        applicationId, service.getId()))
                .toList();

        int healthyCount = countByStatus(serviceHealthResults, "HEALTHY");
        int degradedCount = countByStatus(serviceHealthResults, "DEGRADED");
        int unhealthyCount = countByStatus(serviceHealthResults, "UNHEALTHY");
        int unknownCount = countByStatus(serviceHealthResults, "UNKNOWN");

        String applicationStatus;

        if (unhealthyCount > 0) {
            applicationStatus = "UNHEALTHY";
        } else if (degradedCount > 0) {
            applicationStatus = "DEGRADED";
        } else if (unknownCount > 0 || monitoredServices.isEmpty()) {
            applicationStatus = "UNKNOWN";
        } else {
            applicationStatus = "HEALTHY";
        }

        return new ApplicationHealthResponse(
                applicationId,
                applicationStatus,
                LocalDateTime.now(),
                monitoredServices.size(),
                healthyCount,
                degradedCount,
                unhealthyCount,
                unknownCount,
                serviceHealthResults);
    }

    private int countByStatus(
            List<ServiceHealthResponse> results, String status) {
        return (int) results.stream()
                .filter(result -> status.equals(result.status()))
                .count();
    }

    private Long getAuthenticatedUserId() {
        Object principal = SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        if (principal instanceof Long userId) {
            return userId;
        }

        throw new AccessDeniedException(
                "Authenticated user ID is unavailable");
    }
}
