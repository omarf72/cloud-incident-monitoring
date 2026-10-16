package com.omar.incident_monitoring.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.omar.incident_monitoring.dto.IncidentCreateRequest;
import com.omar.incident_monitoring.dto.IncidentResponse;
import com.omar.incident_monitoring.dto.IncidentUpdateRequest;
import com.omar.incident_monitoring.entity.Application;
import com.omar.incident_monitoring.entity.Incident;
import com.omar.incident_monitoring.repository.ApplicationRepository;
import com.omar.incident_monitoring.repository.IncidentRepository;
import com.omar.incident_monitoring.entity.Incident.IncidentStatus;
import com.omar.incident_monitoring.exception.ApplicationNotFoundException;
import com.omar.incident_monitoring.exception.IncidentNotFound;
import com.omar.incident_monitoring.exception.InvalidIncidentUpdateException;

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

    public List<IncidentResponse> getIncidents(Long applicationId) {
        Long userId = getAuthenticatedUserId();

        Application ownApplication = applicationRepository
                .findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        return incidentRepository.findAllByApplicationId(ownApplication.getId())
                .stream()
                .map(this::convertToResponse)
                .toList();

    }

    public IncidentResponse updateIncident(
            Long incidentId,
            Long applicationId,
            IncidentUpdateRequest request) {

        Long userId = getAuthenticatedUserId();

        // Verify the authenticated user owns the application.
        applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        // Verify the incident belongs to this application.
        Incident incident = incidentRepository
                .findByIdAndApplicationId(incidentId, applicationId)
                .orElseThrow(() -> new IncidentNotFound(incidentId));

        // Update name only if provided.
        if (request.getName() != null) {
            if (request.getName().isBlank()) {
                throw new InvalidIncidentUpdateException(
                        "Incident name cannot be blank");
            }
            if (request.getName().length() > 100) {
                throw new InvalidIncidentUpdateException(
                        "Incident name must be 100 characters or fewer");
            }

            incident.setName(request.getName());
        }

        // Update severity only if provided.
        if (request.getSeverity() != null) {
            incident.setSeverity(request.getSeverity());
        }

        // Update reason only if provided.
        if (request.getReason() != null) {
            if (request.getReason().length() > 1000) {
                throw new InvalidIncidentUpdateException(
                        "Incident reason must be 1000 characters or fewer");
            }

            incident.setReason(request.getReason());
        }

        // Update status only if provided.
        if (request.getStatus() != null) {
            if (incident.getStatus() == IncidentStatus.RESOLVED
                    && request.getStatus() != IncidentStatus.RESOLVED) {
                throw new InvalidIncidentUpdateException(
                        "A resolved incident cannot be reopened");
            }

            // Record the resolution time only on the first transition.
            if (incident.getStatus() != IncidentStatus.RESOLVED
                    && request.getStatus() == IncidentStatus.RESOLVED) {
                incident.setEndedAt(LocalDateTime.now());
            }

            incident.setStatus(request.getStatus());
        }

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
