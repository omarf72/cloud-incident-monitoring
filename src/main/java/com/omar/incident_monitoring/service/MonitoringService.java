package com.omar.incident_monitoring.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.omar.incident_monitoring.dto.MetricValue;
import com.omar.incident_monitoring.dto.MonitoringBaselineResponse;
import com.omar.incident_monitoring.monitoring.prometheus.PrometheusClient;



@Service
public class MonitoringService {

    private final PrometheusClient prometheusClient;
    private final String applicationName;

    public MonitoringService(
            PrometheusClient prometheusClient,
            @Value("${spring.application.name}") String applicationName) {

        this.prometheusClient = prometheusClient;
        this.applicationName = applicationName;
    }

    public MonitoringBaselineResponse getBaseline() {

        String applicationFilter = "{application=\"" + applicationName + "\"}";

        List<MetricValue> metrics = List.of(
                collect(
                        "request_rate",
                        "Request Rate",
                        "sum(rate(http_server_requests_milliseconds_count"
                                + applicationFilter + "[5m]))",
                        "requests/sec",
                        "Average number of HTTP requests per second over five minutes."),

                collect(
                        "error_rate",
                        "HTTP 5xx Error Rate",
                        "100 * sum(rate(http_server_requests_milliseconds_count"
                                + "{application=\"" + applicationName
                                + "\",status=~\"5..\"}[5m]))"
                                + " / clamp_min(sum(rate(http_server_requests_milliseconds_count"
                                + applicationFilter + "[5m])), 1e-9)",
                        "%",
                        "Percentage of HTTP requests resulting in a 5xx response over five minutes."),

                collect(
                        "jvm_memory",
                        "JVM Memory Used",
                        "sum(jvm_memory_used_bytes" + applicationFilter + ")",
                        "bytes",
                        "Total memory currently used by JVM memory pools."),

                collect(
                        "system_cpu",
                        "System CPU Usage",
                        "system_cpu_usage" + applicationFilter + " * 100",
                        "%",
                        "System CPU usage as a percentage."),

                collect(
                        "database_connections",
                        "Active Database Connections",
                        "sum(hikaricp_connections_active" + applicationFilter + ")",
                        "connections",
                        "Number of currently active HikariCP database connections.")
        );

        return new MonitoringBaselineResponse(
                applicationName,
                Instant.now(),
                metrics);
    }

    private MetricValue collect(
            String key,
            String name,
            String query,
            String unit,
            String description) {

        Optional<Double> result = prometheusClient.query(query);

        return new MetricValue(
                key,
                name,
                result.orElse(null),
                unit,
                result.isPresent(),
                description);
    }
}
