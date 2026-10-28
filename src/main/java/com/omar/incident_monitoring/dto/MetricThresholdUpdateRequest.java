package com.omar.incident_monitoring.dto;
import jakarta.validation.constraints.NotNull;

public record MetricThresholdUpdateRequest(
        @NotNull Boolean enabled
) {}
