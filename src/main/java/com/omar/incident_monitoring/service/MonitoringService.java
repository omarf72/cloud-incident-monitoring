package com.omar.incident_monitoring.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.omar.incident_monitoring.dto.MetricHistoryResponse;
import com.omar.incident_monitoring.dto.MetricSample;
import com.omar.incident_monitoring.dto.MetricValue;
import com.omar.incident_monitoring.dto.MonitoringBaselineResponse;
import com.omar.incident_monitoring.monitoring.prometheus.PrometheusClient;
import com.omar.incident_monitoring.monitoring.prometheus.PrometheusSample;

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

                String errorRateQuery = "100 * (sum(rate(http_server_requests_milliseconds_count"
                                + "{application=\"" + applicationName
                                + "\",status=~\"5..\"}[5m])) or vector(0))"
                                + " / clamp_min(sum(rate(http_server_requests_milliseconds_count"
                                + applicationFilter + "[5m])), 1e-9)";

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
                                                errorRateQuery,
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
                                                "Number of currently active HikariCP database connections."));

                return new MonitoringBaselineResponse(
                                applicationName,
                                Instant.now(),
                                metrics);
        }

        public MetricHistoryResponse getHistory(
                        String metricKey,
                        String rangeValue) {

                MonitoringRange range = MonitoringRange.fromValue(rangeValue);

                MetricDefinition definition = getMetricDefinition(metricKey);

                Instant end = Instant.now();
                Instant start = end.minus(range.getDuration());

                List<PrometheusSample> prometheusSamples = prometheusClient.queryRange(
                                definition.query(),
                                start,
                                end,
                                range.getStep());

                List<MetricSample> samples = prometheusSamples.stream()
                                .map(sample -> new MetricSample(
                                                sample.timestamp(),
                                                sample.value()))
                                .toList();

                return new MetricHistoryResponse(
                                metricKey,
                                definition.name(),
                                range.getValue(),
                                definition.unit(),
                                samples);
        }

        private MetricDefinition getMetricDefinition(String metricKey) {

                String applicationFilter = "{application=\"" + applicationName + "\"}";

                return switch (metricKey) {

                        case "request_rate" -> new MetricDefinition(
                                        "Request Rate",
                                        "sum(rate(http_server_requests_milliseconds_count"
                                                        + applicationFilter + "[5m]))",
                                        "requests/sec");

                        case "error_rate" -> new MetricDefinition(
                                        "HTTP 5xx Error Rate",
                                        "100 * (sum(rate(http_server_requests_milliseconds_count"
                                                        + "{application=\"" + applicationName
                                                        + "\",status=~\"5..\"}[5m])) or vector(0))"
                                                        + " / clamp_min(sum(rate(http_server_requests_milliseconds_count"
                                                        + applicationFilter + "[5m])), 1e-9)",
                                        "%");

                        case "jvm_memory" -> new MetricDefinition(
                                        "JVM Memory Used",
                                        "sum(jvm_memory_used_bytes" + applicationFilter + ")",
                                        "bytes");

                        case "system_cpu" -> new MetricDefinition(
                                        "System CPU Usage",
                                        "avg(system_cpu_usage" + applicationFilter + ") * 100",
                                        "%");

                        case "database_connections" -> new MetricDefinition(
                                        "Active Database Connections",
                                        "sum(hikaricp_connections_active" + applicationFilter + ")",
                                        "connections");

                        default -> throw new IllegalArgumentException(
                                        "Unsupported monitoring metric.");
                };
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

        private record MetricDefinition(
                        String name,
                        String query,
                        String unit) {
        }
}