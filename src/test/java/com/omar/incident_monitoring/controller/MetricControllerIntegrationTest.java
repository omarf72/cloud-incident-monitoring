package com.omar.incident_monitoring.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.omar.incident_monitoring.entity.Application;
import com.omar.incident_monitoring.entity.Metric;
import com.omar.incident_monitoring.entity.Metric.MetricType;
import com.omar.incident_monitoring.entity.Service;
import com.omar.incident_monitoring.entity.User;
import com.omar.incident_monitoring.repository.ApplicationRepository;
import com.omar.incident_monitoring.repository.MetricRepository;
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
    
}
