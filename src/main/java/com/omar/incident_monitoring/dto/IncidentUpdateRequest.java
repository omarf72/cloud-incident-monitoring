package com.omar.incident_monitoring.dto;

import com.omar.incident_monitoring.entity.Incident.IncidentSeverity;
import com.omar.incident_monitoring.entity.Incident.IncidentStatus;
import jakarta.validation.constraints.Size;

public class IncidentUpdateRequest {
    
    private IncidentStatus status;

    private IncidentSeverity severity;

    @Size(max = 1000, message = "Reason must be 1000 characters or fewer")
    private String reason;

    @Size(max = 100, message = "Name must be 100 characters or fewer")
    private String name;
    
    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(IncidentSeverity severity) {
        this.severity = severity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    
}
