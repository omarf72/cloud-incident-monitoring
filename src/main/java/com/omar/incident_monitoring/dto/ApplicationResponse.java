package com.omar.incident_monitoring.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder ({
    "id",
    "name",
    "services",
    "experimentsRan"
})

public class ApplicationResponse {
    
    private Long id;

    private String name;

    private List<ServiceResponse> services;

    private Integer experimentsRan;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ServiceResponse> getServices() {
        return services;
    }

    public void setServices(List<ServiceResponse> services) {
        this.services = services;
    }

    public Integer getExperimentsRan() {
        return experimentsRan;
    }

    public void setExperimentsRan(Integer experimentsRan) {
        this.experimentsRan = experimentsRan;
    }

    
}
