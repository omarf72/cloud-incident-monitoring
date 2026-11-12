package com.omar.incident_monitoring.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ServiceHealthResponse(
        Long serviceId,
        String serviceName,
        String status,
        LocalDateTime evaluatedAt,
        int activeIncidentCount,
        List<String> reasons) {
}
