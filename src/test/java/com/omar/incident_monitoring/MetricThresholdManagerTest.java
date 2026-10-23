package com.omar.incident_monitoring;

import com.omar.incident_monitoring.dto.MetricThresholdCreateRequest;
import com.omar.incident_monitoring.dto.MetricThresholdResponse;
import com.omar.incident_monitoring.entity.Application;
import com.omar.incident_monitoring.entity.Metric.MetricType;
import com.omar.incident_monitoring.entity.MetricThreshold.ThresholdOperator;
import com.omar.incident_monitoring.entity.Service;
import com.omar.incident_monitoring.entity.User;
import com.omar.incident_monitoring.repository.ApplicationRepository;
import com.omar.incident_monitoring.repository.MetricThresholdRepository;
import com.omar.incident_monitoring.repository.ServiceRepository;
import com.omar.incident_monitoring.repository.UserRepository;
import com.omar.incident_monitoring.service.MetricThresholdManager;
import com.omar.incident_monitoring.exception.ApplicationNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MetricThresholdManagerTest {

    @Autowired
    private MetricThresholdManager metricThresholdManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private MetricThresholdRepository metricThresholdRepository;

    private User owner;
    private User anotherUser;
    private Application application;
    private Service service;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setEmail("threshold-owner@example.com");
        owner.setPasswordHash("password");
        owner = userRepository.saveAndFlush(owner);

        anotherUser = new User();
        anotherUser.setEmail("threshold-other@example.com");
        anotherUser.setPasswordHash("password");
        anotherUser = userRepository.saveAndFlush(anotherUser);

        application = new Application();
        application.setName("Threshold Test Application");
        application.setUser(owner);
        application = applicationRepository.saveAndFlush(application);

        service = new Service();
        service.setName("Payment API");
        service.setType("API");
        service.setApplication(application);
        service = serviceRepository.saveAndFlush(service);
    }

    @Test
    void ownerCanCreateThresholdForTheirService() {
        MetricThresholdCreateRequest request =
                new MetricThresholdCreateRequest(
                        MetricType.CPU_USAGE_PERCENT,
                        ThresholdOperator.GREATER_THAN,
                        90.0
                );

        MetricThresholdResponse response =
                metricThresholdManager.createThreshold(
                        application.getId(),
                        service.getId(),
                        owner.getId(),
                        request
                );

        assertNotNull(response.id());
        assertEquals(service.getId(), response.serviceId());
        assertEquals(MetricType.CPU_USAGE_PERCENT, response.metricType());
        assertEquals(ThresholdOperator.GREATER_THAN, response.operator());
        assertEquals(90.0, response.thresholdValue());
        assertTrue(response.enabled());

        assertTrue(
                metricThresholdRepository.findByService_Id(service.getId())
                        .stream()
                        .anyMatch(threshold ->
                                threshold.getId().equals(response.id()))
        );
    }

    @Test
    void ownerCanRetrieveThresholdsForTheirService() {
        metricThresholdManager.createThreshold(
                application.getId(),
                service.getId(),
                owner.getId(),
                new MetricThresholdCreateRequest(
                        MetricType.CPU_USAGE_PERCENT,
                        ThresholdOperator.GREATER_THAN,
                        90.0
                )
        );

        metricThresholdManager.createThreshold(
                application.getId(),
                service.getId(),
                owner.getId(),
                new MetricThresholdCreateRequest(
                        MetricType.MEMORY_USAGE_PERCENT,
                        ThresholdOperator.GREATER_THAN,
                        85.0
                )
        );

        List<MetricThresholdResponse> thresholds =
                metricThresholdManager.getThresholds(
                        application.getId(),
                        service.getId(),
                        owner.getId()
                );

        assertEquals(2, thresholds.size());
        assertTrue(thresholds.stream().anyMatch(threshold ->
                threshold.metricType() == MetricType.CPU_USAGE_PERCENT));
        assertTrue(thresholds.stream().anyMatch(threshold ->
                threshold.metricType() == MetricType.MEMORY_USAGE_PERCENT));
    }

    @Test
    void anotherUserCannotCreateOrRetrieveThresholds() {
        MetricThresholdCreateRequest request =
                new MetricThresholdCreateRequest(
                        MetricType.CPU_USAGE_PERCENT,
                        ThresholdOperator.GREATER_THAN,
                        90.0
                );

        assertThrows(ApplicationNotFoundException.class, () ->
                metricThresholdManager.createThreshold(
                        application.getId(),
                        service.getId(),
                        anotherUser.getId(),
                        request
                )
        );

        assertThrows(ApplicationNotFoundException.class, () ->
                metricThresholdManager.getThresholds(
                        application.getId(),
                        service.getId(),
                        anotherUser.getId()
                )
        );

        assertTrue(
                metricThresholdRepository.findByService_Id(service.getId())
                        .isEmpty()
        );
    }

    @Test
    void thresholdAbove100PercentIsRejected() {
        MetricThresholdCreateRequest request =
                new MetricThresholdCreateRequest(
                        MetricType.CPU_USAGE_PERCENT,
                        ThresholdOperator.GREATER_THAN,
                        101.0
                );

        assertThrows(IllegalArgumentException.class, () ->
                metricThresholdManager.createThreshold(
                        application.getId(),
                        service.getId(),
                        owner.getId(),
                        request
                )
        );

        assertTrue(
                metricThresholdRepository.findByService_Id(service.getId())
                        .isEmpty()
        );
    }
}
