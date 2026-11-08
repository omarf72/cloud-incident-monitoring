package com.omar.incident_monitoring.monitoring.prometheus;


import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import tools.jackson.databind.JsonNode;


@Component
public class PrometheusClient {

    private final RestClient restClient;

    public PrometheusClient(
           @Value("${monitoring.prometheus.url:http://localhost:9090}") String prometheusUrl) {

        this.restClient = RestClient.builder()
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
}
