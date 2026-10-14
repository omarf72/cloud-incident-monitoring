package com.omar.incident_monitoring.service;

import org.springframework.stereotype.Service;

import com.omar.incident_monitoring.repository.ApplicationRepository;

import com.omar.incident_monitoring.entity.Application;
import com.omar.incident_monitoring.exception.ApplicationNotFoundException;
import com.omar.incident_monitoring.dto.ApplicationResponse;
import com.omar.incident_monitoring.dto.ServiceResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import com.omar.incident_monitoring.dto.ApplicationCreateRequest;
import com.omar.incident_monitoring.entity.User;
import com.omar.incident_monitoring.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    // helper method that transforms application to a shorter application response
    private ApplicationResponse convertToResponse(Application application) {
        ApplicationResponse response = new ApplicationResponse();

        response.setId(application.getId());
        response.setName(application.getName());

        List<ServiceResponse> serviceResponses =
        application.getServices() == null
                ? new java.util.ArrayList<>()
                : application.getServices().stream()
                    .map(service -> {
                        ServiceResponse serviceResponse =
                                new ServiceResponse();
                        serviceResponse.setId(service.getId());
                        serviceResponse.setName(service.getName());
                        serviceResponse.setType(service.getType());
                        return serviceResponse;
                    })
                    .collect(Collectors.toList());

        response.setServices(serviceResponses);

       response.setExperimentsRan(
        application.getExperiments() == null
                ? 0
                : application.getExperiments().size());

        return response;
    }

    public List<ApplicationResponse> getAllApplications() {
        Long userId = getAuthenticatedUserId();

        return applicationRepository.findAllByUserId(userId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public ApplicationResponse getApplicationById(Long id) {
        Long userId = getAuthenticatedUserId();

        Application application = applicationRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApplicationNotFoundException(id));

        return convertToResponse(application);
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


    @Transactional
    public ApplicationResponse createApplication(
        ApplicationCreateRequest request) {

    Long userId = getAuthenticatedUserId();

    User user = userRepository.findById(userId)
            .orElseThrow(() ->
                    new org.springframework.security.access.AccessDeniedException(
                            "Authenticated user was not found"));

    Application application = new Application();
    application.setName(request.getName().trim());
    application.setUser(user);

    Application savedApplication =
            applicationRepository.save(application);

    return convertToResponse(savedApplication);
}


}
