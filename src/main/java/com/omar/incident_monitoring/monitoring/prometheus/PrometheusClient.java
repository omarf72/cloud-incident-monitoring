package com.omar.incident_monitoring.monitoring.prometheus;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import tools.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class PrometheusClient {

    private final RestClient restClient;

    @Autowired
    public PrometheusClient(
            @Value("${monitoring.prometheus.url:http://localhost:9090}") String prometheusUrl) {
        this(prometheusUrl, RestClient.builder());
    }

    PrometheusClient(String prometheusUrl, RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl(prometheusUrl)
                .build();
    }

    public Optional<Double> query(String promQl) {
        try {
            JsonNode response = restClient.get()
                    .uri("/api/v1/query?query={query}", promQl)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !"success".equals(response.path("status").asText())) {
                throw new PrometheusUnavailableException(
                        "Prometheus returned an unsuccessful query response.");
            }

            JsonNode results = response.path("data").path("result");

            if (!results.isArray()) {
                throw new PrometheusUnavailableException(
                        "Prometheus returned an invalid query result.");
            }

            if (results.isEmpty()) {
                return Optional.empty();
            }

            String value = results.get(0)
                    .path("value")
                    .path(1)
                    .asText();

            return Optional.of(Double.parseDouble(value));

        } catch (RestClientException | NumberFormatException exception) {
            throw new PrometheusUnavailableException(
                    "Unable to retrieve metrics from Prometheus.", exception);
        }
    }

    public List<PrometheusSample> queryRange(
            String promQl,
            Instant start,
            Instant end,
            Duration step) {

        if (start == null || end == null || step == null
                || !start.isBefore(end)
                || step.isZero()
                || step.isNegative()
                || step.toMillis() < 1) {
            throw new IllegalArgumentException(
                    "A valid start, end, and positive sampling interval are required.");
        }

        try {
            String stepSeconds = String.valueOf(
                    step.toMillis() / 1000.0);

            JsonNode response = restClient.get()
                    .uri(
                            "/api/v1/query_range?query={query}&start={start}&end={end}&step={step}",
                            promQl,
                            start.toString(),
                            end.toString(),
                            stepSeconds)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !"success".equals(response.path("status").asText())) {
                throw new PrometheusUnavailableException(
                        "Prometheus returned an unsuccessful range query response.");
            }

            JsonNode data = response.path("data");

            if (!"matrix".equals(data.path("resultType").asText())) {
                throw new PrometheusUnavailableException(
                        "Prometheus returned an invalid range query result type.");
            }

            JsonNode results = data.path("result");

            if (!results.isArray()) {
                throw new PrometheusUnavailableException(
                        "Prometheus returned an invalid range query result.");
            }

            List<PrometheusSample> samples = new ArrayList<>();

            for (JsonNode series : results) {
                JsonNode values = series.path("values");

                if (!values.isArray()) {
                    throw new PrometheusUnavailableException(
                            "Prometheus returned invalid time-series samples.");
                }

                for (JsonNode sample : values) {
                    if (!sample.isArray() || sample.size() < 2) {
                        throw new PrometheusUnavailableException(
                                "Prometheus returned a malformed time-series sample.");
                    }

                    double timestamp = Double.parseDouble(
                            sample.get(0).asText());

                    double value = Double.parseDouble(
                            sample.get(1).asText());

                    Instant instant = Instant.ofEpochMilli(
                            Math.round(timestamp * 1000));

                    samples.add(new PrometheusSample(instant, value));
                }
            }

            samples.sort(Comparator.comparing(PrometheusSample::timestamp));

            return List.copyOf(samples);

        } catch (RestClientException | NumberFormatException | DateTimeException exception) {
            throw new PrometheusUnavailableException(
                    "Unable to retrieve historical metrics from Prometheus.",
                    exception);
        }
    }
}
