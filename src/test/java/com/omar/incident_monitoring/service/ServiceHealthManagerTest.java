package com.omar.incident_monitoring.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.omar.incident_monitoring.dto.ServiceHealthResponse;
import com.omar.incident_monitoring.entity.Application;
import com.omar.incident_monitoring.entity.Incident;
import com.omar.incident_monitoring.entity.Incident.IncidentSeverity;
import com.omar.incident_monitoring.entity.Incident.IncidentStatus;
import com.omar.incident_monitoring.entity.Metric;
import com.omar.incident_monitoring.entity.Service;
import com.omar.incident_monitoring.repository.ApplicationRepository;
import com.omar.incident_monitoring.repository.IncidentRepository;
import com.omar.incident_monitoring.repository.MetricRepository;
import com.omar.incident_monitoring.repository.ServiceRepository;

@ExtendWith(MockitoExtension.class)
class ServiceHealthManagerTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private MetricRepository metricRepository;

    private ServiceHealthManager serviceHealthManager;

    private static final Long USER_ID = 42L;
    private static final Long APPLICATION_ID = 1L;
    private static final Long SERVICE_ID = 10L;

    private Service service;

    @BeforeEach
    void setUp() {
        serviceHealthManager = new ServiceHealthManager(
                applicationRepository,
                serviceRepository,
                incidentRepository,
                metricRepository,
                Duration.ofMinutes(5));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null));

        service = new Service();
        service.setId(SERVICE_ID);
        service.setName("Payments API");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubOwnedService() {
        when(applicationRepository.findByIdAndUserId(
                APPLICATION_ID, USER_ID))
                .thenReturn(Optional.of(new Application()));

        when(serviceRepository.findByIdAndApplicationId(
                SERVICE_ID, APPLICATION_ID))
                .thenReturn(Optional.of(service));
    }

    private void stubNoActiveIncidents() {
        when(incidentRepository.findAllByServiceId(SERVICE_ID))
                .thenReturn(List.of());
    }

    @Test
    void getServiceHealthReturnsHealthyWhenTelemetryIsFreshAndNoIncidentsExist() {
        stubOwnedService();
        stubNoActiveIncidents();

        Metric metric = new Metric();
        metric.setRecordedAt(LocalDateTime.now().minusMinutes(1));

        when(metricRepository.findTopByService_IdOrderByRecordedAtDesc(
                SERVICE_ID))
                .thenReturn(Optional.of(metric));

        ServiceHealthResponse response = serviceHealthManager.getServiceHealth(
                APPLICATION_ID, SERVICE_ID);

        assertEquals("HEALTHY", response.status());
        assertEquals(0, response.activeIncidentCount());
    }

    @Test
    void getServiceHealthReturnsUnknownWhenTelemetryIsMissing() {
        stubOwnedService();
        stubNoActiveIncidents();

        when(metricRepository.findTopByService_IdOrderByRecordedAtDesc(
                SERVICE_ID))
                .thenReturn(Optional.empty());

        ServiceHealthResponse response = serviceHealthManager.getServiceHealth(
                APPLICATION_ID, SERVICE_ID);

        assertEquals("UNKNOWN", response.status());
        assertEquals(0, response.activeIncidentCount());
    }

    @Test
    void getServiceHealthReturnsUnknownWhenTelemetryIsStale() {
        stubOwnedService();
        stubNoActiveIncidents();

        Metric metric = new Metric();
        metric.setRecordedAt(LocalDateTime.now().minusMinutes(10));

        when(metricRepository.findTopByService_IdOrderByRecordedAtDesc(
                SERVICE_ID))
                .thenReturn(Optional.of(metric));

        ServiceHealthResponse response = serviceHealthManager.getServiceHealth(
                APPLICATION_ID, SERVICE_ID);

        assertEquals("UNKNOWN", response.status());
    }

    @Test
    void getServiceHealthReturnsDegradedForActiveWarningIncident() {
        stubOwnedService();

        Incident incident = createIncident(
                IncidentSeverity.MEDIUM,
                IncidentStatus.OPEN);

        when(incidentRepository.findAllByServiceId(SERVICE_ID))
                .thenReturn(List.of(incident));

        ServiceHealthResponse response = serviceHealthManager.getServiceHealth(
                APPLICATION_ID, SERVICE_ID);

        assertEquals("DEGRADED", response.status());
        assertEquals(1, response.activeIncidentCount());
    }

    @Test
    void getServiceHealthReturnsUnhealthyForActiveHighIncidentEvenWithoutTelemetry() {
        stubOwnedService();

        Incident incident = createIncident(
                IncidentSeverity.HIGH,
                IncidentStatus.ACKNOWLEDGED);

        when(incidentRepository.findAllByServiceId(SERVICE_ID))
                .thenReturn(List.of(incident));

        when(metricRepository.findTopByService_IdOrderByRecordedAtDesc(
                SERVICE_ID))
                .thenReturn(Optional.empty());

        ServiceHealthResponse response = serviceHealthManager.getServiceHealth(
                APPLICATION_ID, SERVICE_ID);

        assertEquals("UNHEALTHY", response.status());
        assertEquals(1, response.activeIncidentCount());
    }

    @Test
    void getServiceHealthIgnoresResolvedIncidents() {
        stubOwnedService();

        Incident incident = createIncident(
                IncidentSeverity.CRITICAL,
                IncidentStatus.RESOLVED);

        when(incidentRepository.findAllByServiceId(SERVICE_ID))
                .thenReturn(List.of(incident));

        Metric metric = new Metric();
        metric.setRecordedAt(LocalDateTime.now());

        when(metricRepository.findTopByService_IdOrderByRecordedAtDesc(
                SERVICE_ID))
                .thenReturn(Optional.of(metric));

        ServiceHealthResponse response = serviceHealthManager.getServiceHealth(
                APPLICATION_ID, SERVICE_ID);

        assertEquals("HEALTHY", response.status());
        assertEquals(0, response.activeIncidentCount());
    }

    @Test
    void getServiceHealthReturnsNotFoundForAnotherUsersApplication() {
        when(applicationRepository.findByIdAndUserId(
                APPLICATION_ID, USER_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> serviceHealthManager.getServiceHealth(
                        APPLICATION_ID, SERVICE_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verifyNoInteractions(
                serviceRepository,
                incidentRepository,
                metricRepository);
    }

    private Incident createIncident(
            IncidentSeverity severity,
            IncidentStatus status) {

        Incident incident = new Incident();
        incident.setName("CPU threshold breached");
        incident.setSeverity(severity);
        incident.setStatus(status);
        incident.setReason(
                "CPU usage exceeded the configured threshold");

        return incident;
    }
}