package com.omar.incident_monitoring.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.omar.incident_monitoring.dto.MonitoringBaselineResponse;
import com.omar.incident_monitoring.monitoring.prometheus.PrometheusClient;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.omar.incident_monitoring.dto.MetricHistoryResponse;
import com.omar.incident_monitoring.monitoring.prometheus.PrometheusSample;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

        @Mock
        private PrometheusClient prometheusClient;

        private MonitoringService monitoringService;

        @BeforeEach
        void setUp() {
                monitoringService = new MonitoringService(
                                prometheusClient,
                                "incident-monitoring");
        }

        @Test
        void getBaselineReturnsAvailableMetrics() {
                when(prometheusClient.query(org.mockito.ArgumentMatchers.anyString()))
                                .thenReturn(
                                                Optional.of(12.5),
                                                Optional.of(2.0),
                                                Optional.of(123456.0),
                                                Optional.of(35.0),
                                                Optional.of(4.0));

                MonitoringBaselineResponse response = monitoringService.getBaseline();

                assertEquals("incident-monitoring", response.application());
                assertNotNull(response.collectedAt());
                assertEquals(5, response.metrics().size());

                assertEquals("request_rate", response.metrics().get(0).key());
                assertEquals(12.5, response.metrics().get(0).value());
                assertTrue(response.metrics().get(0).available());

                assertEquals("error_rate", response.metrics().get(1).key());
                assertEquals(2.0, response.metrics().get(1).value());

                assertEquals("system_cpu", response.metrics().get(3).key());
                assertEquals(35.0, response.metrics().get(3).value());
        }

        @Test
        void getBaselineRepresentsMissingMetricsAsUnavailable() {
                when(prometheusClient.query(org.mockito.ArgumentMatchers.anyString()))
                                .thenReturn(
                                                Optional.empty(),
                                                Optional.of(0.0),
                                                Optional.empty(),
                                                Optional.of(0.0),
                                                Optional.empty());

                MonitoringBaselineResponse response = monitoringService.getBaseline();

                assertFalse(response.metrics().get(0).available());
                assertNull(response.metrics().get(0).value());

                // A real zero is still available telemetry.
                assertTrue(response.metrics().get(1).available());
                assertEquals(0.0, response.metrics().get(1).value());

                assertFalse(response.metrics().get(2).available());
                assertFalse(response.metrics().get(4).available());
        }

        @Test
        void getHistoryReturnsHistoricalSamples() {
                Instant timestamp = Instant.parse("2026-11-08T16:00:00Z");

                when(prometheusClient.queryRange(
                                any(),
                                any(Instant.class),
                                any(Instant.class),
                                any(Duration.class)))
                                .thenReturn(List.of(
                                                new PrometheusSample(timestamp, 25.5),
                                                new PrometheusSample(timestamp.plusSeconds(60), 30.2)));

                MetricHistoryResponse response = monitoringService.getHistory(
                                "system_cpu",
                                "1h");

                assertEquals("system_cpu", response.key());
                assertEquals("System CPU Usage", response.name());
                assertEquals("1h", response.range());
                assertEquals("%", response.unit());

                assertEquals(2, response.samples().size());
                assertEquals(timestamp, response.samples().get(0).timestamp());
                assertEquals(25.5, response.samples().get(0).value());
                assertEquals(30.2, response.samples().get(1).value());

                verify(prometheusClient).queryRange(
                                eq("avg(system_cpu_usage{application=\"incident-monitoring\"}) * 100"),
                                any(Instant.class),
                                any(Instant.class),
                                eq(Duration.ofMinutes(1)));
        }

        @Test
        void getHistoryReturnsEmptySamplesWhenNoDataExists() {
                when(prometheusClient.queryRange(
                                any(),
                                any(Instant.class),
                                any(Instant.class),
                                any(Duration.class)))
                                .thenReturn(List.of());

                MetricHistoryResponse response = monitoringService.getHistory(
                                "jvm_memory",
                                "24h");

                assertEquals("jvm_memory", response.key());
                assertTrue(response.samples().isEmpty());

                verify(prometheusClient).queryRange(
                                eq("sum(jvm_memory_used_bytes{application=\"incident-monitoring\"})"),
                                any(Instant.class),
                                any(Instant.class),
                                eq(Duration.ofMinutes(5)));
        }

        @Test
        void getHistoryRejectsUnsupportedMetric() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> monitoringService.getHistory("unknown_metric", "1h"));
        }

        @Test
        void getHistoryRejectsUnsupportedRange() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> monitoringService.getHistory("system_cpu", "30d"));
        }

        @Test
        void getBaselineErrorRateQueryRequiresRequestTelemetry() {
                when(prometheusClient.query(any()))
                                .thenReturn(
                                                Optional.of(10.0),
                                                Optional.of(0.0),
                                                Optional.of(123456.0),
                                                Optional.of(35.0),
                                                Optional.of(4.0));

                monitoringService.getBaseline();

                ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);

                verify(prometheusClient, org.mockito.Mockito.times(5))
                                .query(queryCaptor.capture());

                String errorRateQuery = queryCaptor.getAllValues().get(1);

                assertTrue(errorRateQuery.contains("or (0 * sum(rate("));
                assertTrue(errorRateQuery.contains("clamp_min"));
                assertTrue(errorRateQuery.contains("status=~\"5..\""));
        }

        @Test
        void getBaselineReturnsUnavailableErrorRateWhenRequestTelemetryIsMissing() {
                when(prometheusClient.query(any()))
                                .thenReturn(
                                                Optional.empty(),
                                                Optional.empty(),
                                                Optional.of(123456.0),
                                                Optional.of(35.0),
                                                Optional.of(4.0));

                MonitoringBaselineResponse response = monitoringService.getBaseline();

                assertFalse(response.metrics().get(0).available());
                assertNull(response.metrics().get(0).value());

                assertFalse(response.metrics().get(1).available());
                assertNull(response.metrics().get(1).value());
        }

        @Test
        void getBaselineReturnsZeroErrorRateWhenRequestsExistWithoutServerErrors() {
                when(prometheusClient.query(any()))
                                .thenReturn(
                                                Optional.of(10.0),
                                                Optional.of(0.0),
                                                Optional.of(123456.0),
                                                Optional.of(35.0),
                                                Optional.of(4.0));

                MonitoringBaselineResponse response = monitoringService.getBaseline();

                assertTrue(response.metrics().get(0).available());
                assertEquals(10.0, response.metrics().get(0).value());

                assertTrue(response.metrics().get(1).available());
                assertEquals(0.0, response.metrics().get(1).value());
        }

        @Test
        void getHistoryUsesSharedErrorRateQuery() {
                when(prometheusClient.queryRange(
                                any(),
                                any(Instant.class),
                                any(Instant.class),
                                any(Duration.class)))
                                .thenReturn(List.of());

                monitoringService.getHistory("error_rate", "1h");

                ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);

                verify(prometheusClient).queryRange(
                                queryCaptor.capture(),
                                any(Instant.class),
                                any(Instant.class),
                                any(Duration.class));

                String query = queryCaptor.getValue();

                assertTrue(query.contains("status=~\"5..\""));
                assertTrue(query.contains("or (0 * sum(rate("));
                assertTrue(query.contains("clamp_min"));
                assertTrue(query.contains("application=\"incident-monitoring\""));
        }
}
