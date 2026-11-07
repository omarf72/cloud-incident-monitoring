package com.omar.incident_monitoring.dto;

public record MetricValue(
        String key,
        String name,
        Double value,
        String unit,
        boolean available,
        String description) {
}
