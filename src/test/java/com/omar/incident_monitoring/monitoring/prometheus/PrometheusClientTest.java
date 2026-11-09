package com.omar.incident_monitoring.monitoring.prometheus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PrometheusClientTest {

        private MockRestServiceServer server;
        private PrometheusClient prometheusClient;

        @BeforeEach
        void setUp() {
                RestClient.Builder builder = RestClient.builder();

                server = MockRestServiceServer.bindTo(builder).build();

                prometheusClient = new PrometheusClient(
                                "http://localhost:9090",
                                builder);
        }

        @Test
        void queryRangeReturnsTimestampedSamples() {

                String response = """
                                {
                                    "status": "success",
                                    "data": {
                                        "resultType": "matrix",
                                        "result": [
                                            {
                                                "metric": {},
                                                "values": [
                                                    [1794171600, "25.5"],
                                                    [1794171630, "30.2"]
                                                ]
                                            }
                                        ]
                                    }
                                }
                                """;

                server.expect(requestTo(
                                org.hamcrest.Matchers.startsWith(
                                                "http://localhost:9090/api/v1/query_range?")))
                                .andExpect(method(HttpMethod.GET))
                                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

                List<PrometheusSample> samples = prometheusClient.queryRange(
                                "system_cpu_usage",
                                Instant.parse("2026-11-08T15:00:00Z"),
                                Instant.parse("2026-11-08T16:00:00Z"),
                                Duration.ofSeconds(30));

                assertEquals(2, samples.size());

                assertEquals(
                                Instant.ofEpochSecond(1794171600),
                                samples.get(0).timestamp());

                assertEquals(25.5, samples.get(0).value());

                assertEquals(
                                Instant.ofEpochSecond(1794171630),
                                samples.get(1).timestamp());

                assertEquals(30.2, samples.get(1).value());

                server.verify();
        }

        @Test
        void queryRangeReturnsEmptyListWhenNoDataExists() {

                String response = """
                                {
                                    "status": "success",
                                    "data": {
                                        "resultType": "matrix",
                                        "result": []
                                    }
                                }
                                """;

                server.expect(requestTo(
                                org.hamcrest.Matchers.startsWith(
                                                "http://localhost:9090/api/v1/query_range?")))
                                .andExpect(method(HttpMethod.GET))
                                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

                List<PrometheusSample> samples = prometheusClient.queryRange(
                                "system_cpu_usage",
                                Instant.parse("2026-11-08T15:00:00Z"),
                                Instant.parse("2026-11-08T16:00:00Z"),
                                Duration.ofSeconds(30));

                assertTrue(samples.isEmpty());

                server.verify();
        }

        @Test
        void queryRangeRejectsInvalidTimeRange() {

                Instant start = Instant.parse("2026-11-08T16:00:00Z");
                Instant end = Instant.parse("2026-11-08T15:00:00Z");

                assertThrows(
                                IllegalArgumentException.class,
                                () -> prometheusClient.queryRange(
                                                "system_cpu_usage",
                                                start,
                                                end,
                                                Duration.ofSeconds(30)));
        }

        @Test
        void queryRangeRejectsZeroSamplingInterval() {

                Instant start = Instant.parse("2026-11-08T15:00:00Z");
                Instant end = Instant.parse("2026-11-08T16:00:00Z");

                assertThrows(
                                IllegalArgumentException.class,
                                () -> prometheusClient.queryRange(
                                                "system_cpu_usage",
                                                start,
                                                end,
                                                Duration.ZERO));
        }
}
