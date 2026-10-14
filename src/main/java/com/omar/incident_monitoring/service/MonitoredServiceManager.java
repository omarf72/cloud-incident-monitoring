package com.omar.incident_monitoring.service;

import com.omar.incident_monitoring.dto.ServiceCreateRequest;
import com.omar.incident_monitoring.entity.Application;
import com.omar.incident_monitoring.entity.Service;
import com.omar.incident_monitoring.exception.ApplicationNotFoundException;
import com.omar.incident_monitoring.repository.ApplicationRepository;
import com.omar.incident_monitoring.repository.ServiceRepository;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import com.omar.incident_monitoring.dto.MonitoredServiceResponse;
import com.omar.incident_monitoring.exception.ServiceNotFoundException;

@org.springframework.stereotype.Service
public class MonitoredServiceManager {

    private final ServiceRepository serviceRepository;
    private final ApplicationRepository applicationRepository;

    public MonitoredServiceManager(
            ServiceRepository serviceRepository,
            ApplicationRepository applicationRepository) {
        this.serviceRepository = serviceRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public MonitoredServiceResponse createService(
            Long applicationId, ServiceCreateRequest request) {

        Long userId = getAuthenticatedUserId();

        Application application = applicationRepository
                .findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        Service service = new Service();
        service.setName(request.getName().trim());
        service.setType(request.getType().trim().toUpperCase());
        service.setApplication(application);

        Service savedService = serviceRepository.save(service);

        return convertToResponse(savedService);
    }

    public List<MonitoredServiceResponse> getServicesForApplication(
            Long applicationId) {

        Long userId = getAuthenticatedUserId();

        applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        return serviceRepository.findAllByApplicationId(applicationId)
                .stream()
                .map(this::convertToResponse)
                .toList();
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

    private MonitoredServiceResponse convertToResponse(Service service) {
        MonitoredServiceResponse response = new MonitoredServiceResponse();

        response.setId(service.getId());
        response.setName(service.getName());
        response.setType(service.getType());
        response.setCreatedAt(service.getCreatedAt());

        return response;
    }

    @Transactional
    public MonitoredServiceResponse updateService(
            Long applicationId,
            Long serviceId,
            ServiceCreateRequest request) {

        Long userId = getAuthenticatedUserId();

        // Verify the authenticated user owns the application.
        applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        // Verify the service belongs to this application.
        Service service = serviceRepository
                .findByIdAndApplicationId(serviceId, applicationId)
                .orElseThrow(() -> new ServiceNotFoundException(serviceId));

        // Apply the requested changes.
        service.setName(request.getName().trim());
        service.setType(request.getType().trim().toUpperCase());

        Service updatedService = serviceRepository.save(service);

        return convertToResponse(updatedService);
    }

    @Transactional 
    public void deleteService(Long applicationId,Long serviceId){

        Long userId = getAuthenticatedUserId();

         // Verify the authenticated user owns the application.
        applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        // Verify the service belongs to this application.
        Service service = serviceRepository
                .findByIdAndApplicationId(serviceId, applicationId)
                .orElseThrow(() -> new ServiceNotFoundException(serviceId));
        
        serviceRepository.delete(service);
    }

}
