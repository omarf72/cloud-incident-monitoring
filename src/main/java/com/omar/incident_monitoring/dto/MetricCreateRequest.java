package com.omar.incident_monitoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;


public record MetricCreateRequest( 
    
    @NotBlank(message = "Metric type is required")
    String type,

    @NotNull(message = "Metric value is required")
    @PositiveOrZero(message = "Metric value cannot be negative")
    Double value

) {}
