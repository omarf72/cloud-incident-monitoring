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
}
