package com.omar.incident_monitoring.controller;

import com.omar.incident_monitoring.entity.Application;
import com.omar.incident_monitoring.entity.Service;
import com.omar.incident_monitoring.entity.User;
import com.omar.incident_monitoring.repository.ApplicationRepository;
import com.omar.incident_monitoring.repository.MetricThresholdRepository;
import com.omar.incident_monitoring.repository.ServiceRepository;
import com.omar.incident_monitoring.repository.UserRepository;
import com.omar.incident_monitoring.service.JwtService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MetricThresholdControllerIntegrationTest {

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
    private MetricThresholdRepository metricThresholdRepository;

    @Test
    void userCanCreateThresholdUnderTheirService() throws Exception {
        User owner = createUser("threshold-owner");
        Application application = createApplication(owner);
        Service service = createService(application);

        String token = jwtService.generateToken(owner);

        mockMvc.perform(post(thresholdUrl(application, service))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "metricType": "CPU_USAGE_PERCENT",
                            "operator": "GREATER_THAN",
                            "thresholdValue": 90
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.serviceId")
                        .value(service.getId()))
                .andExpect(jsonPath("$.metricType")
                        .value("CPU_USAGE_PERCENT"))
                .andExpect(jsonPath("$.operator")
                        .value("GREATER_THAN"))
                .andExpect(jsonPath("$.thresholdValue").value(90.0))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertEquals(
                1,
                metricThresholdRepository
                        .findByService_Id(service.getId())
                        .size());
    }

    @Test
    void userCanRetrieveThresholdsForTheirService() throws Exception {
        User owner = createUser("threshold-history");
        Application application = createApplication(owner);
        Service service = createService(application);

        String token = jwtService.generateToken(owner);

        // Create the first threshold.
        mockMvc.perform(post(thresholdUrl(application, service))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "metricType": "CPU_USAGE_PERCENT",
                            "operator": "GREATER_THAN",
                            "thresholdValue": 90
                        }
                        """))
                .andExpect(status().isCreated());

        // Create the second threshold.
        mockMvc.perform(post(thresholdUrl(application, service))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "metricType": "MEMORY_USAGE_PERCENT",
                            "operator": "GREATER_THAN",
                            "thresholdValue": 85
                        }
                        """))
                .andExpect(status().isCreated());

        // Retrieve both thresholds.
        mockMvc.perform(get(thresholdUrl(application, service))
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath(
                        "$[?(@.metricType == 'CPU_USAGE_PERCENT')]").isNotEmpty())
                .andExpect(jsonPath(
                        "$[?(@.metricType == 'MEMORY_USAGE_PERCENT')]").isNotEmpty());
    }

    @Test
    void userCannotCreateThresholdUnderAnotherUsersApplication() throws Exception {
        User owner = createUser("threshold-real-owner");
        Application application = createApplication(owner);
        Service service = createService(application);

        User otherUser = createUser("threshold-other-user");
        String token = jwtService.generateToken(otherUser);

        mockMvc.perform(post(thresholdUrl(application, service))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "metricType": "CPU_USAGE_PERCENT",
                            "operator": "GREATER_THAN",
                            "thresholdValue": 90
                        }
                        """))
                .andExpect(status().isNotFound());

        assertTrue(
                metricThresholdRepository
                        .findByService_Id(service.getId())
                        .isEmpty());
    }

    @Test
    void thresholdAbove100PercentIsRejected() throws Exception {
        User owner = createUser("threshold-invalid");
        Application application = createApplication(owner);
        Service service = createService(application);

        String token = jwtService.generateToken(owner);

        mockMvc.perform(post(thresholdUrl(application, service))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "metricType": "CPU_USAGE_PERCENT",
                            "operator": "GREATER_THAN",
                            "thresholdValue": 101
                        }
                        """))
                .andExpect(status().isBadRequest());

        assertTrue(
                metricThresholdRepository
                        .findByService_Id(service.getId())
                        .isEmpty());
    }

    private User createUser(String prefix) {
        User user = new User();
        user.setEmail(prefix + "-" + UUID.randomUUID() + "@example.com");
        user.setPasswordHash("password");
        return userRepository.saveAndFlush(user);
    }

    private Application createApplication(User owner) {
        Application application = new Application();
        application.setName("Threshold Test Application");
        application.setUser(owner);
        return applicationRepository.saveAndFlush(application);
    }

    private Service createService(Application application) {
        Service service = new Service();
        service.setName("Payment API");
        service.setType("API");
        service.setApplication(application);
        return serviceRepository.saveAndFlush(service);
    }

    private String thresholdUrl(Application application, Service service) {
        return "/api/applications/" + application.getId()
                + "/services/" + service.getId()
                + "/thresholds";
    }
}
