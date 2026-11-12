package com.omar.incident_monitoring.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ApplicationHealthResponse(
        Long applicationId,
        String status,
        LocalDateTime evaluatedAt,
        int totalServices,
        int healthyServiceCount,
        int degradedServiceCount,
        int unhealthyServiceCount,
        int unknownServiceCount,
        List<ServiceHealthResponse> services
) {}