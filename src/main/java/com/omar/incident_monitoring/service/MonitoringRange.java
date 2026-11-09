package com.omar.incident_monitoring.service;

import java.time.Duration;
import java.util.Arrays;

public enum MonitoringRange {

    FIFTEEN_MINUTES("15m", Duration.ofMinutes(15), Duration.ofSeconds(30)),
    ONE_HOUR("1h", Duration.ofHours(1), Duration.ofMinutes(1)),
    SIX_HOURS("6h", Duration.ofHours(6), Duration.ofMinutes(2)),
    TWENTY_FOUR_HOURS("24h", Duration.ofHours(24), Duration.ofMinutes(5)),
    SEVEN_DAYS("7d", Duration.ofDays(7), Duration.ofMinutes(15));

    private final String value;
    private final Duration duration;
    private final Duration step;

    MonitoringRange(String value, Duration duration, Duration step) {
        this.value = value;
        this.duration = duration;
        this.step = step;
    }

    public String getValue() {
        return value;
    }

    public Duration getDuration() {
        return duration;
    }

    public Duration getStep() {
        return step;
    }

    public static MonitoringRange fromValue(String value) {
        return Arrays.stream(values())
                .filter(range -> range.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported monitoring time range."));
    }
}
