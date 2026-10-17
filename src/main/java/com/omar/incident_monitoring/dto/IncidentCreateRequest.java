package com.omar.incident_monitoring.dto;

import com.omar.incident_monitoring.entity.Incident;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class IncidentCreateRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be 100 character or under")
    private String name;

    @NotNull
    private Incident.IncidentSeverity severity;

    @NotNull
    private Incident.IncidentStatus status;

    @Size(max = 1000, message = "Reason must be 1000 or under")
    private String reason;

    private Long serviceId;

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Incident.IncidentSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(Incident.IncidentSeverity severity) {
        this.severity = severity;
    }

    public Incident.IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(Incident.IncidentStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

}
