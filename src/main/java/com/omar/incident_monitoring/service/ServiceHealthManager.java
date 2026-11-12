package com.omar.incident_monitoring.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.omar.incident_monitoring.dto.ServiceHealthResponse;
import com.omar.incident_monitoring.entity.Incident;
import com.omar.incident_monitoring.entity.Incident.IncidentSeverity;
import com.omar.incident_monitoring.entity.Incident.IncidentStatus;
import com.omar.incident_monitoring.entity.Metric;
//import com.omar.incident_monitoring.entity.Service;
import com.omar.incident_monitoring.repository.ApplicationRepository;
import com.omar.incident_monitoring.repository.IncidentRepository;
import com.omar.incident_monitoring.repository.MetricRepository;
import com.omar.incident_monitoring.repository.ServiceRepository;

@Service
public class ServiceHealthManager {

    private final ApplicationRepository applicationRepository;
    private final ServiceRepository serviceRepository;
    private final IncidentRepository incidentRepository;
    private final MetricRepository metricRepository;
    private final Duration freshnessWindow;

    public ServiceHealthManager(
            ApplicationRepository applicationRepository,
            ServiceRepository serviceRepository,
            IncidentRepository incidentRepository,
            MetricRepository metricRepository,
            @Value("${service-health.telemetry-freshness:5m}") Duration freshnessWindow) {

        this.applicationRepository = applicationRepository;
        this.serviceRepository = serviceRepository;
        this.incidentRepository = incidentRepository;
        this.metricRepository = metricRepository;
        this.freshnessWindow = freshnessWindow;
    }

    public ServiceHealthResponse getServiceHealth(
            Long applicationId,
            Long serviceId) {

        Long userId = getAuthenticatedUserId();

        applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application not found"));

        com.omar.incident_monitoring.entity.Service service = serviceRepository
                .findByIdAndApplicationId(serviceId, applicationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Service not found"));

        LocalDateTime evaluatedAt = LocalDateTime.now();

        List<Incident> activeIncidents = incidentRepository
                .findAllByServiceId(serviceId)
                .stream()
                .filter(incident -> incident.getStatus() == IncidentStatus.OPEN
                        || incident.getStatus() == IncidentStatus.ACKNOWLEDGED)
                .toList();

        List<String> reasons = new ArrayList<>();

        boolean hasCriticalIncident = activeIncidents.stream()
                .anyMatch(incident -> incident.getSeverity() == IncidentSeverity.HIGH
                        || incident.getSeverity() == IncidentSeverity.CRITICAL);

        boolean hasWarningIncident = activeIncidents.stream()
                .anyMatch(incident -> incident.getSeverity() == IncidentSeverity.LOW
                        || incident.getSeverity() == IncidentSeverity.MEDIUM);

        for (Incident incident : activeIncidents) {
            String reason = incident.getName();

            if (incident.getReason() != null
                    && !incident.getReason().isBlank()) {
                reason += ": " + incident.getReason();
            }

            reasons.add(reason);
        }

        Metric latestMetric = metricRepository
                .findTopByService_IdOrderByRecordedAtDesc(serviceId)
                .orElse(null);

        boolean telemetryIsFresh = latestMetric != null
                && latestMetric.getRecordedAt() != null
                && !latestMetric.getRecordedAt()
                        .isBefore(evaluatedAt.minus(freshnessWindow));

        String status;

        if (hasCriticalIncident) {
            status = "UNHEALTHY";
        } else if (hasWarningIncident) {
            status = "DEGRADED";
        } else if (!telemetryIsFresh) {
            status = "UNKNOWN";

            reasons.add("No recent telemetry was received within "
                    + freshnessWindow + ".");
        } else {
            status = "HEALTHY";

            reasons.add("Recent telemetry is available and there are no active incidents.");
        }

        return new ServiceHealthResponse(
                service.getId(),
                service.getName(),
                status,
                evaluatedAt,
                activeIncidents.size(),
                reasons);
    }

    private Long getAuthenticatedUserId() {
        Object principal = SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        if (principal instanceof Long userId) {
            return userId;
        }

        throw new AccessDeniedException("Authenticated user ID is unavailable");
    }
}
