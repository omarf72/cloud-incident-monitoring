package com.omar.incident_monitoring.dto;

import java.time.Instant;
import java.util.List;

public record MonitoringBaselineResponse(
        String application,
        Instant collectedAt,
        List<MetricValue> metrics) {
}
