package com.omar.incident_monitoring.dto;

import java.time.Instant;

public record MetricSample(
                Instant timestamp,
                double value) {
}
