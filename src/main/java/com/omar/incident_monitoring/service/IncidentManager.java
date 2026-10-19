package com.omar.incident_monitoring.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.omar.incident_monitoring.dto.IncidentCreateRequest;
import com.omar.incident_monitoring.dto.IncidentResponse;
import com.omar.incident_monitoring.dto.IncidentUpdateRequest;
import com.omar.incident_monitoring.entity.Application;
import com.omar.incident_monitoring.entity.Incident;
import com.omar.incident_monitoring.repository.ApplicationRepository;
import com.omar.incident_monitoring.repository.IncidentRepository;
import com.omar.incident_monitoring.repository.ServiceRepository;
import com.omar.incident_monitoring.entity.Incident.IncidentStatus;
import com.omar.incident_monitoring.exception.ApplicationNotFoundException;
import com.omar.incident_monitoring.exception.IncidentNotFound;
import com.omar.incident_monitoring.exception.InvalidIncidentUpdateException;
import com.omar.incident_monitoring.exception.ServiceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import com.omar.incident_monitoring.dto.IncidentPageResponse;

@Service
public class IncidentManager {

    private final ApplicationRepository applicationRepository;

    private final IncidentRepository incidentRepository;

    private final ServiceRepository serviceRepository;

    public IncidentManager(
            ApplicationRepository applicationRepository,
            IncidentRepository incidentRepository,
            ServiceRepository serviceRepository) {
        this.applicationRepository = applicationRepository;
        this.incidentRepository = incidentRepository;
        this.serviceRepository = serviceRepository;
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

        com.omar.incident_monitoring.entity.Service service = null;

        if (request.getServiceId() != null) {
            service = serviceRepository
                    .findByIdAndApplicationId(
                            request.getServiceId(),
                            applicationId)
                    .orElseThrow(
                            () -> new ServiceNotFoundException(
                                    request.getServiceId()));
        }

        Incident incident = new Incident();

        incident.setName(request.getName());
        incident.setSeverity(request.getSeverity());
        incident.setStatus(IncidentStatus.OPEN);
        incident.setReason(request.getReason());
        incident.setApplication(ownApplication);
        incident.setService(service);

        LocalDateTime now = LocalDateTime.now();
        incident.setCreatedAt(now);
        incident.setStartedAt(now);

        Incident savedIncident = incidentRepository.save(incident);

        return convertToResponse(savedIncident);
    }

    public IncidentPageResponse getIncidents(
            Long applicationId,
            Incident.IncidentStatus status,
            Incident.IncidentSeverity severity, int page,
            int size,String sort) {

        Long userId = getAuthenticatedUserId();

        Application ownApplication = applicationRepository
                .findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        String[] sortParts = sort.split(",");

        if (sortParts.length != 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sort must use the format field,direction");
        }

        String sortField = sortParts[0].trim();
        String sortDirection = sortParts[1].trim();

        List<String> allowedSortFields = List.of(
                "createdAt",
                "startedAt",
                "severity",
                "status",
                "name");

        if (!allowedSortFields.contains(sortField)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid sort field: " + sortField);
        }

        Sort.Direction direction;

        try {
            direction = Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sort direction must be asc or desc");
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortField));

        Page<Incident> incidentPage = incidentRepository.findByApplicationIdAndOptionalFilters(
                ownApplication.getId(), status, severity, pageable);

        List<IncidentResponse> incidents = incidentPage.getContent()
                .stream()
                .map(this::convertToResponse)
                .toList();

        return new IncidentPageResponse(
                incidents,
                incidentPage.getNumber(),
                incidentPage.getSize(),
                incidentPage.getTotalElements(),
                incidentPage.getTotalPages(),
                incidentPage.isFirst(),
                incidentPage.isLast());
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
