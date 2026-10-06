package com.omar.incident_monitoring.service;

import org.springframework.stereotype.Service;

import com.omar.incident_monitoring.repository.ApplicationRepository;

import com.omar.incident_monitoring.entity.Application;
import com.omar.incident_monitoring.exception.ApplicationNotFoundException;
import com.omar.incident_monitoring.dto.ApplicationResponse;
import com.omar.incident_monitoring.dto.ServiceResponse;

import java.util.List;
import java.util.stream.Collectors;

@Service 
public class ApplicationService  {

    private final ApplicationRepository applicationRepository;

    public ApplicationService(ApplicationRepository applicationRepository)
    {
        this.applicationRepository=applicationRepository;
    }

    //helper method that transforms application to a shorter application response
    private ApplicationResponse convertToResponse(Application application){
         ApplicationResponse response = new ApplicationResponse();

    response.setId(application.getId());
    response.setName(application.getName());

    List<ServiceResponse> serviceResponses =
        application.getServices().stream()
            .map(service -> {

                ServiceResponse serviceResponse = new ServiceResponse();

                serviceResponse.setId(service.getId());
                serviceResponse.setName(service.getName());
                serviceResponse.setType(service.getType());

                return serviceResponse;
            })
            .collect(Collectors.toList());

    response.setServices(serviceResponses);

    response.setExperimentsRan(
        application.getExperiments().size()
    );

    return response;
    }

    public List<ApplicationResponse> getAllApplications() {

        return applicationRepository.findAll()
            .stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }


    public ApplicationResponse getApplicationById(Long id){
        Application application=applicationRepository.findById(id)
        .orElseThrow(()-> new ApplicationNotFoundException(id));

        return convertToResponse(application);
    }
    
}
