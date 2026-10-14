package com.omar.incident_monitoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ServiceCreateRequest {

    @NotBlank(message = "Service name is required")
    @Size(max = 100, message = "Service name must be 100 characters or fewer")
    private String name;

    @NotBlank(message = "Service type is required")
    @Size(max = 50, message = "Service type must be 50 characters or fewer")
    private String type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
