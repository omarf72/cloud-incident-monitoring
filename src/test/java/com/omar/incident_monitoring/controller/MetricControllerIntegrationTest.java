package com.omar.incident_monitoring.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.omar.incident_monitoring.entity.Application;
import com.omar.incident_monitoring.entity.Incident;
import com.omar.incident_monitoring.entity.Incident.IncidentStatus;
import com.omar.incident_monitoring.entity.Metric;
import com.omar.incident_monitoring.entity.Metric.MetricType;
import com.omar.incident_monitoring.entity.MetricThreshold;
import com.omar.incident_monitoring.entity.MetricThreshold.ThresholdOperator;
import com.omar.incident_monitoring.entity.Service;
import com.omar.incident_monitoring.entity.User;
import com.omar.incident_monitoring.repository.ApplicationRepository;
import com.omar.incident_monitoring.repository.IncidentRepository;
import com.omar.incident_monitoring.repository.MetricRepository;
import com.omar.incident_monitoring.repository.MetricThresholdRepository;
import com.omar.incident_monitoring.repository.ServiceRepository;
import com.omar.incident_monitoring.repository.UserRepository;
import com.omar.incident_monitoring.service.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
class MetricControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private MetricRepository metricRepository;

    @Autowired
    private MetricThresholdRepository metricThresholdRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Test
    void userCanCreateMetricUnderTheirService() throws Exception {
        User owner = new User();
        owner.setEmail("metric-owner@example.com");
        owner.setPasswordHash("password");
        owner = userRepository.saveAndFlush(owner);

        Application application = new Application();
        application.setName("Metric Test Application");
        application.setUser(owner);
        application = applicationRepository.saveAndFlush(application);

        Service service = new Service();
        service.setName("Payment");
        service.setType("API");
        service.setApplication(application);
        service = serviceRepository.saveAndFlush(service);

        String token = jwtService.generateToken(owner);

        mockMvc.perform(post(
                "/api/applications/" + application.getId()
                        + "/services/" + service.getId() + "/metrics")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "type": "CPU_USAGE_PERCENT",
                            "value": 42.5
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.applicationId")
                        .value(application.getId()))
                .andExpect(jsonPath("$.serviceId").value(service.getId()))
                .andExpect(jsonPath("$.type").value("CPU_USAGE_PERCENT"))
                .andExpect(jsonPath("$.value").value(42.5))
                .andExpect(jsonPath("$.recordedAt").isNotEmpty());

        org.junit.jupiter.api.Assertions.assertEquals(
                1,
                metricRepository
                        .findByApplicationIdAndServiceIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                                application.getId(),
                                service.getId(),
                                java.time.LocalDateTime.now().minusMinutes(1),
                                java.time.LocalDateTime.now().plusMinutes(1))
                        .size());
    }

    @Test
    void userCannotCreateMetricUnderAnotherUsersApplication() throws Exception {
        User owner = new User();
        owner.setEmail("metric-real-owner@example.com");
        owner.setPasswordHash("password");
        owner = userRepository.saveAndFlush(owner);

        Application application = new Application();
        application.setName("Private Metric Application");
        application.setUser(owner);
        application = applicationRepository.saveAndFlush(application);

        Service service = new Service();
        service.setName("Private API");
        service.setType("API");
        service.setApplication(application);
        service = serviceRepository.saveAndFlush(service);

        User otherUser = new User();
        otherUser.setEmail("metric-other-user@example.com");
        otherUser.setPasswordHash("password");
        otherUser = userRepository.saveAndFlush(otherUser);

        String token = jwtService.generateToken(otherUser);

        mockMvc.perform(post(
                "/api/applications/" + application.getId()
                        + "/services/" + service.getId() + "/metrics")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "type": "CPU_USAGE_PERCENT",
                            "value": 42.5
                        }
                        """))
                .andExpect(status().isNotFound());

        org.junit.jupiter.api.Assertions.assertTrue(
                metricRepository
                        .findByApplicationIdAndServiceIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                                application.getId(),
                                service.getId(),
                                java.time.LocalDateTime.now().minusMinutes(1),
                                java.time.LocalDateTime.now().plusMinutes(1))
                        .isEmpty());
    }

    @Test
    void userCanRetrieveMetricsWithinTimeRangeInAscendingOrder() throws Exception {
        User owner = new User();
        owner.setEmail("metric-history@example.com");
        owner.setPasswordHash("password");
        owner = userRepository.saveAndFlush(owner);

        Application application = new Application();
        application.setName("Metric History Application");
        application.setUser(owner);
        application = applicationRepository.saveAndFlush(application);

        Service service = new Service();
        service.setName("Payment");
        service.setType("API");
        service.setApplication(application);
        service = serviceRepository.saveAndFlush(service);

        java.time.LocalDateTime start = java.time.LocalDateTime.of(2026, 10, 20, 10, 0);
        java.time.LocalDateTime middle = start.plusMinutes(5);
        java.time.LocalDateTime end = start.plusMinutes(10);

        Metric earlier = new Metric();
        earlier.setApplication(application);
        earlier.setService(service);
        earlier.setType(MetricType.CPU_USAGE_PERCENT);
        earlier.setValue(35.0);
        earlier.setRecordedAt(middle);
        metricRepository.saveAndFlush(earlier);

        Metric later = new Metric();
        later.setApplication(application);
        later.setService(service);
        later.setType(MetricType.CPU_USAGE_PERCENT);
        later.setValue(65.0);
        later.setRecordedAt(start.plusMinutes(8));
        metricRepository.saveAndFlush(later);

        Metric outsideRange = new Metric();
        outsideRange.setApplication(application);
        outsideRange.setService(service);
        outsideRange.setType(MetricType.CPU_USAGE_PERCENT);
        outsideRange.setValue(99.0);
        outsideRange.setRecordedAt(end.plusMinutes(1));
        metricRepository.saveAndFlush(outsideRange);

        String token = jwtService.generateToken(owner);

        mockMvc.perform(get(
                "/api/applications/" + application.getId()
                        + "/services/" + service.getId() + "/metrics")
                .header("Authorization", "Bearer " + token)
                .param("start", start.toString())
                .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].value").value(35.0))
                .andExpect(jsonPath("$[1].value").value(65.0));
    }

    @Test
    void userCannotRetrieveMetricsFromAnotherUsersApplication() throws Exception {
        User owner = new User();
        owner.setEmail("metric-history-owner@example.com");
        owner.setPasswordHash("password");
        owner = userRepository.saveAndFlush(owner);

        Application application = new Application();
        application.setName("Private Metric History Application");
        application.setUser(owner);
        application = applicationRepository.saveAndFlush(application);

        Service service = new Service();
        service.setName("Private API");
        service.setType("API");
        service.setApplication(application);
        service = serviceRepository.saveAndFlush(service);

        User otherUser = new User();
        otherUser.setEmail("metric-history-other@example.com");
        otherUser.setPasswordHash("password");
        otherUser = userRepository.saveAndFlush(otherUser);

        String token = jwtService.generateToken(otherUser);

        mockMvc.perform(get(
                "/api/applications/" + application.getId()
                        + "/services/" + service.getId() + "/metrics")
                .header("Authorization", "Bearer " + token)
                .param("start", "2026-10-20T10:00:00")
                .param("end", "2026-10-20T11:00:00"))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotCreateMetricWithPercentageAbove100() throws Exception {
        User owner = new User();
        owner.setEmail("metric-invalid-percent@example.com");
        owner.setPasswordHash("password");
        owner = userRepository.saveAndFlush(owner);

        Application application = new Application();
        application.setName("Invalid Percentage Test");
        application.setUser(owner);
        application = applicationRepository.saveAndFlush(application);

        Service service = new Service();
        service.setName("API");
        service.setType("API");
        service.setApplication(application);
        service = serviceRepository.saveAndFlush(service);

        String token = jwtService.generateToken(owner);

        mockMvc.perform(post(
                "/api/applications/" + application.getId()
                        + "/services/" + service.getId() + "/metrics")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "type": "CPU_USAGE_PERCENT",
                            "value": 101
                        }
                        """))
                .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertTrue(
                metricRepository
                        .findByApplicationIdAndServiceIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                                application.getId(),
                                service.getId(),
                                java.time.LocalDateTime.now().minusMinutes(1),
                                java.time.LocalDateTime.now().plusMinutes(1))
                        .isEmpty());
    }

    @Test
    void userCannotCreateMetricWithNegativeValue() throws Exception {
        User owner = new User();
        owner.setEmail("metric-negative-value@example.com");
        owner.setPasswordHash("password");
        owner = userRepository.saveAndFlush(owner);

        Application application = new Application();
        application.setName("Negative Value Test");
        application.setUser(owner);
        application = applicationRepository.saveAndFlush(application);

        Service service = new Service();
        service.setName("API");
        service.setType("API");
        service.setApplication(application);
        service = serviceRepository.saveAndFlush(service);

        String token = jwtService.generateToken(owner);

        mockMvc.perform(post(
                "/api/applications/" + application.getId()
                        + "/services/" + service.getId() + "/metrics")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "type": "LATENCY_MS",
                            "value": -5
                        }
                        """))
                .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertTrue(
                metricRepository
                        .findByApplicationIdAndServiceIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                                application.getId(),
                                service.getId(),
                                java.time.LocalDateTime.now().minusMinutes(1),
                                java.time.LocalDateTime.now().plusMinutes(1))
                        .isEmpty());
    }

    @Test
    void userCannotCreateMetricWithUnsupportedType() throws Exception {
        User owner = new User();
        owner.setEmail("metric-unsupported@example.com");
        owner.setPasswordHash("password");
        owner = userRepository.saveAndFlush(owner);

        Application application = new Application();
        application.setName("Unsupported Metric Test");
        application.setUser(owner);
        application = applicationRepository.saveAndFlush(application);

        Service service = new Service();
        service.setName("API");
        service.setType("API");
        service.setApplication(application);
        service = serviceRepository.saveAndFlush(service);

        String token = jwtService.generateToken(owner);

        mockMvc.perform(post(
                "/api/applications/" + application.getId()
                        + "/services/" + service.getId() + "/metrics")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "type": "CPU_USGE",
                            "value": 42
                        }
                        """))
                .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertTrue(
                metricRepository
                        .findByApplicationIdAndServiceIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                                application.getId(),
                                service.getId(),
                                java.time.LocalDateTime.now().minusMinutes(1),
                                java.time.LocalDateTime.now().plusMinutes(1))
                        .isEmpty());
    }

    @Test
    void breachedThresholdAutomaticallyCreatesIncident() throws Exception {
        User owner = new User();
        owner.setEmail("threshold-breach-owner@example.com");
        owner.setPasswordHash("password");
        owner = userRepository.saveAndFlush(owner);

        Application application = new Application();
        application.setName("Threshold Breach Test");
        application.setUser(owner);
        application = applicationRepository.saveAndFlush(application);

        Service service = new Service();
        service.setName("Payment");
        service.setType("API");
        service.setApplication(application);
        service = serviceRepository.saveAndFlush(service);

        MetricThreshold threshold = new MetricThreshold();
        threshold.setService(service);
        threshold.setMetricType(MetricType.CPU_USAGE_PERCENT);
        threshold.setOperator(ThresholdOperator.GREATER_THAN);
        threshold.setThresholdValue(90.0);
        threshold.setEnabled(true);
        metricThresholdRepository.saveAndFlush(threshold);

        String token = jwtService.generateToken(owner);

        mockMvc.perform(post(
                "/api/applications/" + application.getId()
                        + "/services/" + service.getId() + "/metrics")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "type": "CPU_USAGE_PERCENT",
                            "value": 95.0
                        }
                        """))
                .andExpect(status().isOk());

        List<Incident> incidents = incidentRepository.findAllByServiceId(service.getId());

        org.junit.jupiter.api.Assertions.assertEquals(1, incidents.size());

        Incident incident = incidents.get(0);

        org.junit.jupiter.api.Assertions.assertEquals(
                threshold.getId(),
                incident.getThreshold().getId());

        org.junit.jupiter.api.Assertions.assertEquals(
                IncidentStatus.OPEN,
                incident.getStatus());

        org.junit.jupiter.api.Assertions.assertEquals(
                Incident.IncidentSeverity.HIGH,
                incident.getSeverity());
    }

    @Test
    void repeatedBreachesDoNotCreateDuplicateActiveIncidents() throws Exception {
        User owner = new User();
        owner.setEmail("duplicate-breach-owner@example.com");
        owner.setPasswordHash("password");
        owner = userRepository.saveAndFlush(owner);

        Application application = new Application();
        application.setName("Duplicate Breach Test");
        application.setUser(owner);
        application = applicationRepository.saveAndFlush(application);

        Service service = new Service();
        service.setName("Payment");
        service.setType("API");
        service.setApplication(application);
        service = serviceRepository.saveAndFlush(service);

        MetricThreshold threshold = new MetricThreshold();
        threshold.setService(service);
        threshold.setMetricType(MetricType.CPU_USAGE_PERCENT);
        threshold.setOperator(ThresholdOperator.GREATER_THAN);
        threshold.setThresholdValue(90.0);
        threshold.setEnabled(true);
        metricThresholdRepository.saveAndFlush(threshold);

        String token = jwtService.generateToken(owner);
        String url = "/api/applications/" + application.getId()
                + "/services/" + service.getId() + "/metrics";

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "type": "CPU_USAGE_PERCENT",
                                "value": 95.0
                            }
                            """))
                    .andExpect(status().isOk());
        }

        List<Incident> incidents = incidentRepository.findAllByServiceId(service.getId());

        org.junit.jupiter.api.Assertions.assertEquals(1, incidents.size());
        org.junit.jupiter.api.Assertions.assertEquals(
                IncidentStatus.OPEN, incidents.get(0).getStatus());
    }

    @Test
    void metricBelowThresholdDoesNotCreateIncident() throws Exception {
        User owner = new User();
        owner.setEmail("normal-metric-owner@example.com");
        owner.setPasswordHash("password");
        owner = userRepository.saveAndFlush(owner);

        Application application = new Application();
        application.setName("Normal Metric Test");
        application.setUser(owner);
        application = applicationRepository.saveAndFlush(application);

        Service service = new Service();
        service.setName("Payment");
        service.setType("API");
        service.setApplication(application);
        service = serviceRepository.saveAndFlush(service);

        MetricThreshold threshold = new MetricThreshold();
        threshold.setService(service);
        threshold.setMetricType(MetricType.CPU_USAGE_PERCENT);
        threshold.setOperator(ThresholdOperator.GREATER_THAN);
        threshold.setThresholdValue(90.0);
        threshold.setEnabled(true);
        metricThresholdRepository.saveAndFlush(threshold);

        String token = jwtService.generateToken(owner);

        mockMvc.perform(post(
                "/api/applications/" + application.getId()
                        + "/services/" + service.getId() + "/metrics")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "type": "CPU_USAGE_PERCENT",
                            "value": 75.0
                        }
                        """))
                .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertTrue(
                incidentRepository.findAllByServiceId(service.getId()).isEmpty());
    }

    @Test
    void breachAfterPreviousIncidentIsResolvedCreatesNewIncident() throws Exception {
        User owner = new User();
        owner.setEmail("resolved-breach-owner@example.com");
        owner.setPasswordHash("password");
        owner = userRepository.saveAndFlush(owner);

        Application application = new Application();
        application.setName("Resolved Breach Test");
        application.setUser(owner);
        application = applicationRepository.saveAndFlush(application);

        Service service = new Service();
        service.setName("Payment");
        service.setType("API");
        service.setApplication(application);
        service = serviceRepository.saveAndFlush(service);

        MetricThreshold threshold = new MetricThreshold();
        threshold.setService(service);
        threshold.setMetricType(MetricType.CPU_USAGE_PERCENT);
        threshold.setOperator(ThresholdOperator.GREATER_THAN);
        threshold.setThresholdValue(90.0);
        threshold.setEnabled(true);
        metricThresholdRepository.saveAndFlush(threshold);

        String token = jwtService.generateToken(owner);
        String url = "/api/applications/" + application.getId()
                + "/services/" + service.getId() + "/metrics";

        String breachedMetric = """
                {
                    "type": "CPU_USAGE_PERCENT",
                    "value": 95.0
                }
                """;

        // First breach creates an incident.
        mockMvc.perform(post(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(breachedMetric))
                .andExpect(status().isOk());

        List<Incident> firstIncidents = incidentRepository.findAllByServiceId(service.getId());

        org.junit.jupiter.api.Assertions.assertEquals(1, firstIncidents.size());

        Incident firstIncident = firstIncidents.get(0);
        org.junit.jupiter.api.Assertions.assertEquals(
                IncidentStatus.OPEN, firstIncident.getStatus());

        // Resolve the existing incident through your incident API.
        mockMvc.perform(patch(
                "/api/applications/" + application.getId()
                        + "/incidents/" + firstIncident.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "name": "CPU_USAGE_PERCENT Threshold Breached",
                            "severity": "HIGH",
                            "status": "RESOLVED",
                            "reason": "CPU usage returned to an acceptable level"
                        }
                        """))
                .andExpect(status().isOk());

        // A later breach should create a second incident.
        mockMvc.perform(post(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(breachedMetric))
                .andExpect(status().isOk());

        List<Incident> allIncidents = incidentRepository.findAllByServiceId(service.getId());

        org.junit.jupiter.api.Assertions.assertEquals(2, allIncidents.size());

        long openIncidents = allIncidents.stream()
                .filter(i -> i.getStatus() == IncidentStatus.OPEN)
                .count();

        long resolvedIncidents = allIncidents.stream()
                .filter(i -> i.getStatus() == IncidentStatus.RESOLVED)
                .count();

        org.junit.jupiter.api.Assertions.assertEquals(1, openIncidents);
        org.junit.jupiter.api.Assertions.assertEquals(1, resolvedIncidents);
    }

}
