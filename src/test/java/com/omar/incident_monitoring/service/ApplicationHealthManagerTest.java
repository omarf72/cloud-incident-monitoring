package com.omar.incident_monitoring.service;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

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
import com.omar.incident_monitoring.entity.Service;
import com.omar.incident_monitoring.repository.ApplicationRepository;
import com.omar.incident_monitoring.repository.ServiceRepository;

@ExtendWith(MockitoExtension.class)
class ApplicationHealthManagerTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private ServiceHealthManager serviceHealthManager;

    private ApplicationHealthManager applicationHealthManager;

    private static final Long USER_ID = 42L;
    private static final Long APPLICATION_ID = 1L;

    @BeforeEach
    void setUp() {
        applicationHealthManager = new ApplicationHealthManager(
                applicationRepository,
                serviceRepository,
                serviceHealthManager);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void stubOwnedApplication() {
        when(applicationRepository.findByIdAndUserId(APPLICATION_ID, USER_ID))
                .thenReturn(Optional.of(new Application()));
    }

    private Service createService(Long id) {
        Service service = new Service();
        service.setId(id);
        service.setName("Service " + id);
        return service;
    }

    private ServiceHealthResponse createHealth(Long serviceId, String status) {
        return new ServiceHealthResponse(
                serviceId,
                "Service " + serviceId,
                status,
                java.time.LocalDateTime.now(),
                0,
                List.of());
    }

    @Test
    void returnsHealthyWhenAllServicesAreHealthy() {
        stubOwnedApplication();

        when(serviceRepository.findAllByApplicationId(APPLICATION_ID))
                .thenReturn(List.of(createService(10L), createService(11L)));

        when(serviceHealthManager.getServiceHealth(APPLICATION_ID, 10L))
                .thenReturn(createHealth(10L, "HEALTHY"));

        when(serviceHealthManager.getServiceHealth(APPLICATION_ID, 11L))
                .thenReturn(createHealth(11L, "HEALTHY"));

        var response = applicationHealthManager.getApplicationHealth(APPLICATION_ID);

        assertEquals("HEALTHY", response.status());
        assertEquals(2, response.totalServices());
        assertEquals(2, response.healthyServiceCount());
        assertEquals(0, response.degradedServiceCount());
        assertEquals(0, response.unhealthyServiceCount());
        assertEquals(0, response.unknownServiceCount());
        assertEquals(2, response.services().size());
    }

    @Test
    void returnsUnhealthyWhenAtLeastOneServiceIsUnhealthy() {
        stubOwnedApplication();

        when(serviceRepository.findAllByApplicationId(APPLICATION_ID))
                .thenReturn(List.of(createService(10L), createService(11L)));

        when(serviceHealthManager.getServiceHealth(APPLICATION_ID, 10L))
                .thenReturn(createHealth(10L, "HEALTHY"));

        when(serviceHealthManager.getServiceHealth(APPLICATION_ID, 11L))
                .thenReturn(createHealth(11L, "UNHEALTHY"));

        var response = applicationHealthManager.getApplicationHealth(APPLICATION_ID);

        assertEquals("UNHEALTHY", response.status());
        assertEquals(1, response.healthyServiceCount());
        assertEquals(1, response.unhealthyServiceCount());
    }

    @Test
    void returnsDegradedWhenNoServiceIsUnhealthyButOneIsDegraded() {
        stubOwnedApplication();

        when(serviceRepository.findAllByApplicationId(APPLICATION_ID))
                .thenReturn(List.of(createService(10L), createService(11L)));

        when(serviceHealthManager.getServiceHealth(APPLICATION_ID, 10L))
                .thenReturn(createHealth(10L, "HEALTHY"));

        when(serviceHealthManager.getServiceHealth(APPLICATION_ID, 11L))
                .thenReturn(createHealth(11L, "DEGRADED"));

        var response = applicationHealthManager.getApplicationHealth(APPLICATION_ID);

        assertEquals("DEGRADED", response.status());
        assertEquals(1, response.healthyServiceCount());
        assertEquals(1, response.degradedServiceCount());
    }

    @Test
    void returnsUnknownWhenAtLeastOneServiceIsUnknownAndNoneAreWorse() {
        stubOwnedApplication();

        when(serviceRepository.findAllByApplicationId(APPLICATION_ID))
                .thenReturn(List.of(createService(10L), createService(11L)));

        when(serviceHealthManager.getServiceHealth(APPLICATION_ID, 10L))
                .thenReturn(createHealth(10L, "HEALTHY"));

        when(serviceHealthManager.getServiceHealth(APPLICATION_ID, 11L))
                .thenReturn(createHealth(11L, "UNKNOWN"));

        var response = applicationHealthManager.getApplicationHealth(APPLICATION_ID);

        assertEquals("UNKNOWN", response.status());
        assertEquals(1, response.healthyServiceCount());
        assertEquals(1, response.unknownServiceCount());
    }

    @Test
    void unhealthyTakesPriorityOverDegradedAndUnknown() {
        stubOwnedApplication();

        when(serviceRepository.findAllByApplicationId(APPLICATION_ID))
                .thenReturn(List.of(
                        createService(10L),
                        createService(11L),
                        createService(12L)));

        when(serviceHealthManager.getServiceHealth(APPLICATION_ID, 10L))
                .thenReturn(createHealth(10L, "DEGRADED"));

        when(serviceHealthManager.getServiceHealth(APPLICATION_ID, 11L))
                .thenReturn(createHealth(11L, "UNKNOWN"));

        when(serviceHealthManager.getServiceHealth(APPLICATION_ID, 12L))
                .thenReturn(createHealth(12L, "UNHEALTHY"));

        var response = applicationHealthManager.getApplicationHealth(APPLICATION_ID);

        assertEquals("UNHEALTHY", response.status());
        assertEquals(1, response.degradedServiceCount());
        assertEquals(1, response.unknownServiceCount());
        assertEquals(1, response.unhealthyServiceCount());
    }

    @Test
    void returnsUnknownWhenApplicationHasNoServices() {
        stubOwnedApplication();

        when(serviceRepository.findAllByApplicationId(APPLICATION_ID))
                .thenReturn(List.of());

        var response = applicationHealthManager.getApplicationHealth(APPLICATION_ID);

        assertEquals("UNKNOWN", response.status());
        assertEquals(0, response.totalServices());
        assertEquals(0, response.services().size());

        verifyNoInteractions(serviceHealthManager);
    }

    @Test
    void returnsNotFoundWhenApplicationDoesNotBelongToUser() {
        when(applicationRepository.findByIdAndUserId(APPLICATION_ID, USER_ID))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> applicationHealthManager.getApplicationHealth(APPLICATION_ID));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verifyNoInteractions(serviceRepository, serviceHealthManager);
    }
}
