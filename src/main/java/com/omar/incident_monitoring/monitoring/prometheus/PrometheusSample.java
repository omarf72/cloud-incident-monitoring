package com.omar.incident_monitoring.monitoring.prometheus;

import java.time.Instant;

public record PrometheusSample(
                Instant timestamp,
                double value) {
}
