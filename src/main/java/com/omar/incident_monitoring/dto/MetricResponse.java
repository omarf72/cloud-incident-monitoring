package com.omar.incident_monitoring.dto;

import java.time.LocalDateTime;

public record MetricResponse( Long id,
        Long applicationId,
        Long serviceId,
        String type,
        Double value,
        LocalDateTime recordedAt
)  {}
