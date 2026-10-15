package com.omar.incident_monitoring.service;

import java.time.LocalDateTime;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.omar.incident_monitoring.dto.IncidentCreateRequest;
import com.omar.incident_monitoring.dto.IncidentResponse;
import com.omar.incident_monitoring.entity.Application;
import com.omar.incident_monitoring.entity.Incident;
import com.omar.incident_monitoring.repository.ApplicationRepository;
import com.omar.incident_monitoring.repository.IncidentRepository;
import com.omar.incident_monitoring.entity.Incident.IncidentStatus;
import com.omar.incident_monitoring.exception.ApplicationNotFoundException;

@Service
public class IncidentManager {

    private final ApplicationRepository applicationRepository;

    private final IncidentRepository incidentRepository;

    public IncidentManager(ApplicationRepository applicationRepository, IncidentRepository incidentRepository) {
        this.applicationRepository = applicationRepository;
        this.incidentRepository = incidentRepository;
    }

    private Long getAuthenticatedUserId() {
        Object principal = SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();

        if (principal instanceof Long userId) {
            return userId;
        }

        throw new org.springframework.security.access.AccessDeniedException(
                "Authenticated user ID is unavailable");

    }

    public IncidentResponse createIncident(
        Long applicationId,
        IncidentCreateRequest request) {

    Long userId = getAuthenticatedUserId();

    Application ownApplication = applicationRepository
            .findByIdAndUserId(applicationId, userId)
            .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

    Incident incident = new Incident();

    incident.setName(request.getName());
    incident.setSeverity(request.getSeverity());
    incident.setStatus(IncidentStatus.OPEN);
    incident.setReason(request.getReason());
    incident.setApplication(ownApplication);

    LocalDateTime now = LocalDateTime.now();
    incident.setCreatedAt(now);
    incident.setStartedAt(now);

    Incident savedIncident = incidentRepository.save(incident);

    return convertToResponse(savedIncident);
}

    private IncidentResponse convertToResponse(Incident incident) {
        IncidentResponse response = new IncidentResponse();

        response.setId(incident.getId());
        response.setName(incident.getName());
        response.setSeverity(incident.getSeverity());
        response.setStatus(incident.getStatus());
        response.setReason(incident.getReason());
        response.setApplicationId(incident.getApplication().getId());
        response.setStartedAt(incident.getStartedAt());
        response.setEndedAt(incident.getEndedAt());
        response.setCreatedAt(incident.getCreatedAt());
        response.setServiceId(
                incident.getService() != null
                        ? incident.getService().getId()
                        : null);

        return response;
    }

}
