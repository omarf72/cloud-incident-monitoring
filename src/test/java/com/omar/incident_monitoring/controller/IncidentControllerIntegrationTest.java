package com.omar.incident_monitoring.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.omar.incident_monitoring.entity.Application;
import com.omar.incident_monitoring.entity.Incident;
import com.omar.incident_monitoring.entity.Service;
import com.omar.incident_monitoring.entity.User;
import com.omar.incident_monitoring.repository.ApplicationRepository;
import com.omar.incident_monitoring.repository.IncidentRepository;
import com.omar.incident_monitoring.repository.ServiceRepository;
import com.omar.incident_monitoring.repository.UserRepository;
import com.omar.incident_monitoring.service.JwtService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import com.omar.incident_monitoring.dto.ApplicationCreateRequest;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

@SpringBootTest
@AutoConfigureMockMvc
class IncidentControllerIntegrationTest {

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
        private IncidentRepository incidentRepository;

        @Test
        void userCanCreateIncidentUnderTheirApplication() throws Exception {
                User owner = new User();
                owner.setEmail("incident@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Incident Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                Service service = new Service();
                service.setName("Payment");
                service.setType("API");
                service.setApplication(application);
                service = serviceRepository.saveAndFlush(service);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(post("/api/applications/" + application.getId() + "/incidents")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Elevated API latency",
                                                    "severity": "HIGH",
                                                    "status": "OPEN",
                                                    "reason": "Response times exceeded the configured threshold",
                                                    "serviceId": %d
                                                }
                                                """.formatted(service.getId())))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value("Elevated API latency"))
                                .andExpect(jsonPath("$.severity").value("HIGH"))
                                .andExpect(jsonPath("$.status").value("OPEN"))
                                .andExpect(jsonPath("$.reason").value(
                                                "Response times exceeded the configured threshold"))
                                .andExpect(jsonPath("$.applicationId").value(application.getId()))
                                .andExpect(jsonPath("$.serviceId").value(service.getId()))
                                .andExpect(jsonPath("$.startedAt").isNotEmpty())
                                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                                .andExpect(jsonPath("$.endedAt").doesNotExist());
        }

        @Test
        void anotherUserCannotCreateIncidentUnderSomeoneElsesApplication() throws Exception {
                User owner = new User();
                owner.setEmail("incident2@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Other Cant Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                User other = new User();

                other.setEmail("someonelse@exampl.com");
                other.setPasswordHash("password");
                other = userRepository.saveAndFlush(other);

                String token = jwtService.generateToken(other);

                mockMvc.perform(post("/api/applications/" + application.getId() + "/incidents")
                                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Elevated API latency",
                                                    "severity": "HIGH",
                                                    "status": "OPEN",
                                                    "reason": "Response times exceeded the configured threshold"
                                                }
                                                """))
                                .andExpect(status().isNotFound());

                assertTrue(incidentRepository.findAllByApplicationId(application.getId()).isEmpty());
        }

        @Test
        void userCannotCreateIncidentWithBlankName() throws Exception {
                User owner = new User();
                owner.setEmail("blankincident@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Blank Incident Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(post("/api/applications/" + application.getId() + "/incidents")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "   ",
                                                    "severity": "HIGH",
                                                    "reason": "Response times exceeded the configured threshold"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                assertTrue(
                                incidentRepository.findAllByApplicationId(application.getId()).isEmpty());
        }

        @Test
        void userCannotCreateIncidentWithoutSeverity() throws Exception {
                User owner = new User();
                owner.setEmail("missingseverity@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Missing Severity Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(post("/api/applications/" + application.getId() + "/incidents")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Elevated API latency",
                                                    "reason": "Response times exceeded the configured threshold"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                assertTrue(
                                incidentRepository.findAllByApplicationId(application.getId()).isEmpty());
        }

        @Test
        void userCannotCreateIncidentWithNameExceeding100Characters() throws Exception {
                User owner = new User();
                owner.setEmail("longincidentname@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Long Incident Name Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                String token = jwtService.generateToken(owner);
                String longName = "A".repeat(101);

                mockMvc.perform(post("/api/applications/" + application.getId() + "/incidents")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "%s",
                                                    "severity": "HIGH",
                                                    "reason": "Response times exceeded the configured threshold"
                                                }
                                                """.formatted(longName)))
                                .andExpect(status().isBadRequest());

                assertTrue(
                                incidentRepository.findAllByApplicationId(application.getId()).isEmpty());
        }

        @Test
        void userCanListIncidentsUnderTheirApplication() throws Exception {
                User owner = new User();
                owner.setEmail("listincidents@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("List Incidents Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                Incident incident = new Incident();
                incident.setName("Elevated API latency");
                incident.setSeverity(Incident.IncidentSeverity.HIGH);
                incident.setStatus(Incident.IncidentStatus.OPEN);
                incident.setReason("Response times exceeded the configured threshold");
                incident.setApplication(application);

                LocalDateTime now = LocalDateTime.now();
                incident.setCreatedAt(now);
                incident.setStartedAt(now);
                incident = incidentRepository.saveAndFlush(incident);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(get("/api/applications/" + application.getId() + "/incidents")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content.length()").value(1))
                                .andExpect(jsonPath("$.content[0].id").value(incident.getId()))
                                .andExpect(jsonPath("$.content[0].name").value("Elevated API latency"))
                                .andExpect(jsonPath("$.content[0].severity").value("HIGH"))
                                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                                .andExpect(jsonPath("$.content[0].applicationId").value(application.getId()))
                                .andExpect(jsonPath("$.content[0].reason").value(
                                                "Response times exceeded the configured threshold"))
                                .andExpect(jsonPath("$.page").value(0))
                                .andExpect(jsonPath("$.size").value(10))
                                .andExpect(jsonPath("$.totalElements").value(1))
                                .andExpect(jsonPath("$.totalPages").value(1))
                                .andExpect(jsonPath("$.first").value(true))
                                .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        void anotherUserCannotListIncidentsUnderSomeoneElsesApplication() throws Exception {
                User owner = new User();
                owner.setEmail("incidentlistowner@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Private Incidents Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                Incident incident = new Incident();
                incident.setName("Private incident");
                incident.setSeverity(Incident.IncidentSeverity.HIGH);
                incident.setStatus(Incident.IncidentStatus.OPEN);
                incident.setApplication(application);
                incident.setCreatedAt(LocalDateTime.now());
                incident.setStartedAt(LocalDateTime.now());
                incidentRepository.saveAndFlush(incident);

                User other = new User();
                other.setEmail("differentincidentuser@example.com");
                other.setPasswordHash("password");
                other = userRepository.saveAndFlush(other);

                String token = jwtService.generateToken(other);

                mockMvc.perform(get("/api/applications/" + application.getId() + "/incidents")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound());
        }

        @Test
        void userCanListIncidentsWhenApplicationHasNone() throws Exception {
                User owner = new User();
                owner.setEmail("noincidents@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Application Without Incidents");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(get("/api/applications/" + application.getId() + "/incidents")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.content.length()").value(0))
                                .andExpect(jsonPath("$.totalElements").value(0))
                                .andExpect(jsonPath("$.totalPages").value(0))
                                .andExpect(jsonPath("$.first").value(true))
                                .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        void userCanResolveIncidentAndSetEndedAt() throws Exception {
                User owner = new User();
                owner.setEmail("resolveincident@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Resolve Incident Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                Incident incident = new Incident();
                incident.setName("Elevated API latency");
                incident.setSeverity(Incident.IncidentSeverity.HIGH);
                incident.setStatus(Incident.IncidentStatus.OPEN);
                incident.setReason("Response times exceeded the configured threshold");
                incident.setApplication(application);
                incident.setStartedAt(LocalDateTime.now());
                incident.setCreatedAt(LocalDateTime.now());
                incident = incidentRepository.saveAndFlush(incident);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(patch(
                                "/api/applications/" + application.getId()
                                                + "/incidents/" + incident.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "status": "RESOLVED"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("RESOLVED"))
                                .andExpect(jsonPath("$.endedAt").isNotEmpty());

                Incident updatedIncident = incidentRepository.findById(incident.getId())
                                .orElseThrow();

                assertEquals(Incident.IncidentStatus.RESOLVED, updatedIncident.getStatus());
                assertNotNull(updatedIncident.getEndedAt());
        }

        @Test
        void userCannotReopenResolvedIncident() throws Exception {
                User owner = new User();
                owner.setEmail("reopenincident@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Resolved Incident Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                Incident incident = new Incident();
                incident.setName("Resolved database outage");
                incident.setSeverity(Incident.IncidentSeverity.CRITICAL);
                incident.setStatus(Incident.IncidentStatus.RESOLVED);
                incident.setReason("Database outage was fixed");
                incident.setApplication(application);
                incident.setStartedAt(LocalDateTime.now().minusHours(1));
                incident.setCreatedAt(LocalDateTime.now().minusHours(1));
                incident.setEndedAt(LocalDateTime.now().minusMinutes(10));
                incident = incidentRepository.saveAndFlush(incident);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(patch(
                                "/api/applications/" + application.getId()
                                                + "/incidents/" + incident.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "status": "OPEN"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());

                Incident unchangedIncident = incidentRepository.findById(incident.getId())
                                .orElseThrow();

                assertEquals(Incident.IncidentStatus.RESOLVED, unchangedIncident.getStatus());
                assertNotNull(unchangedIncident.getEndedAt());
        }

        @Test
        void userCanPartiallyUpdateIncident() throws Exception {
                User owner = new User();
                owner.setEmail("partialupdate@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Partial Update Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                Incident incident = new Incident();
                incident.setName("Original incident name");
                incident.setSeverity(Incident.IncidentSeverity.LOW);
                incident.setStatus(Incident.IncidentStatus.OPEN);
                incident.setReason("Original reason");
                incident.setApplication(application);
                incident.setStartedAt(LocalDateTime.now().minusMinutes(30));
                incident.setCreatedAt(LocalDateTime.now().minusMinutes(30));
                incident = incidentRepository.saveAndFlush(incident);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(patch(
                                "/api/applications/" + application.getId()
                                                + "/incidents/" + incident.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "severity": "CRITICAL",
                                                    "reason": "Database connection failures"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Original incident name"))
                                .andExpect(jsonPath("$.severity").value("CRITICAL"))
                                .andExpect(jsonPath("$.status").value("OPEN"))
                                .andExpect(jsonPath("$.reason")
                                                .value("Database connection failures"));

                Incident updatedIncident = incidentRepository.findById(incident.getId())
                                .orElseThrow();

                assertEquals("Original incident name", updatedIncident.getName());
                assertEquals(Incident.IncidentSeverity.CRITICAL,
                                updatedIncident.getSeverity());
                assertEquals(Incident.IncidentStatus.OPEN, updatedIncident.getStatus());
                assertEquals("Database connection failures", updatedIncident.getReason());
        }

        @Test
        void anotherUserCannotUpdateSomeoneElsesIncident() throws Exception {
                User owner = new User();
                owner.setEmail("incidentowner@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                User anotherUser = new User();
                anotherUser.setEmail("otherincidentuser@example.com");
                anotherUser.setPasswordHash("password");
                anotherUser = userRepository.saveAndFlush(anotherUser);

                Application application = new Application();
                application.setName("Owned Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                Incident incident = new Incident();
                incident.setName("Unauthorized Update Test");
                incident.setSeverity(Incident.IncidentSeverity.HIGH);
                incident.setStatus(Incident.IncidentStatus.OPEN);
                incident.setReason("Original reason");
                incident.setApplication(application);
                incident.setStartedAt(LocalDateTime.now().minusMinutes(30));
                incident.setCreatedAt(LocalDateTime.now().minusMinutes(30));
                incident = incidentRepository.saveAndFlush(incident);

                String token = jwtService.generateToken(anotherUser);

                mockMvc.perform(patch(
                                "/api/applications/" + application.getId()
                                                + "/incidents/" + incident.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "status": "RESOLVED"
                                                }
                                                """))
                                .andExpect(status().isNotFound());

                Incident unchangedIncident = incidentRepository.findById(incident.getId())
                                .orElseThrow();

                assertEquals(Incident.IncidentStatus.OPEN, unchangedIncident.getStatus());
                assertEquals("Original reason", unchangedIncident.getReason());
                org.junit.jupiter.api.Assertions.assertNull(unchangedIncident.getEndedAt());
        }

        @Test
        void userCannotAssociateIncidentWithServiceFromAnotherApplication() throws Exception {
                User owner = new User();
                owner.setEmail("incident-owner@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application ownApplication = new Application();
                ownApplication.setName("Own Application");
                ownApplication.setUser(owner);
                ownApplication = applicationRepository.saveAndFlush(ownApplication);

                User anotherOwner = new User();
                anotherOwner.setEmail("another-owner@example.com");
                anotherOwner.setPasswordHash("password");
                anotherOwner = userRepository.saveAndFlush(anotherOwner);

                Application anotherApplication = new Application();
                anotherApplication.setName("Another Application");
                anotherApplication.setUser(anotherOwner);
                anotherApplication = applicationRepository.saveAndFlush(anotherApplication);

                Service anotherService = new Service();
                anotherService.setName("Private API");
                anotherService.setType("API");
                anotherService.setApplication(anotherApplication);
                anotherService = serviceRepository.saveAndFlush(anotherService);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(post("/api/applications/" + ownApplication.getId() + "/incidents")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Unauthorized incident",
                                                    "severity": "HIGH",
                                                    "status": "OPEN",
                                                    "reason": "Testing service ownership",
                                                    "serviceId": %d
                                                }
                                                """.formatted(anotherService.getId())))
                                .andExpect(status().isNotFound());

                assertTrue(
                                incidentRepository.findAllByApplicationId(ownApplication.getId()).isEmpty());
        }

        @Test
        void userCanFilterIncidentsByService() throws Exception {
                User owner = new User();
                owner.setEmail("filter-owner@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Filtering Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                Service paymentService = new Service();
                paymentService.setName("Payment");
                paymentService.setType("API");
                paymentService.setApplication(application);
                paymentService = serviceRepository.saveAndFlush(paymentService);

                Service databaseService = new Service();
                databaseService.setName("Database");
                databaseService.setType("DATABASE");
                databaseService.setApplication(application);
                databaseService = serviceRepository.saveAndFlush(databaseService);

                Incident firstIncident = new Incident();
                firstIncident.setName("Payment latency");
                firstIncident.setSeverity(Incident.IncidentSeverity.HIGH);
                firstIncident.setStatus(Incident.IncidentStatus.OPEN);
                firstIncident.setApplication(application);
                firstIncident.setService(paymentService);
                incidentRepository.saveAndFlush(firstIncident);

                Incident secondIncident = new Incident();
                secondIncident.setName("Payment errors");
                secondIncident.setSeverity(Incident.IncidentSeverity.MEDIUM);
                secondIncident.setStatus(Incident.IncidentStatus.OPEN);
                secondIncident.setApplication(application);
                secondIncident.setService(paymentService);
                incidentRepository.saveAndFlush(secondIncident);

                Incident unrelatedIncident = new Incident();
                unrelatedIncident.setName("Database outage");
                unrelatedIncident.setSeverity(Incident.IncidentSeverity.CRITICAL);
                unrelatedIncident.setStatus(Incident.IncidentStatus.OPEN);
                unrelatedIncident.setApplication(application);
                unrelatedIncident.setService(databaseService);
                incidentRepository.saveAndFlush(unrelatedIncident);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(get("/api/applications/" + application.getId()
                                + "/services/" + paymentService.getId() + "/incidents")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[*].serviceId")
                                                .value(org.hamcrest.Matchers.everyItem(
                                                                org.hamcrest.Matchers.is(
                                                                                paymentService.getId().intValue()))));
        }

        @Test
        void userCannotViewIncidentsForAnotherUsersService() throws Exception {

                User owner = new User();
                owner.setEmail("service-owner@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application ownApplication = new Application();
                ownApplication.setName("Own Application");
                ownApplication.setUser(owner);
                ownApplication = applicationRepository.saveAndFlush(ownApplication);

                User anotherOwner = new User();
                anotherOwner.setEmail("other-service-owner@example.com");
                anotherOwner.setPasswordHash("password");
                anotherOwner = userRepository.saveAndFlush(anotherOwner);

                Application anotherApplication = new Application();
                anotherApplication.setName("Another Application");
                anotherApplication.setUser(anotherOwner);
                anotherApplication = applicationRepository.saveAndFlush(anotherApplication);

                Service privateService = new Service();
                privateService.setName("Private API");
                privateService.setType("API");
                privateService.setApplication(anotherApplication);
                privateService = serviceRepository.saveAndFlush(privateService);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(get("/api/applications/" + ownApplication.getId()
                                + "/services/" + privateService.getId() + "/incidents")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound());
        }

        @Test
        void userCanFilterIncidentsByStatusAndSeverity() throws Exception {
                User owner = new User();
                owner.setEmail("filter-status-severity@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Filtering Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                Incident openHigh = new Incident();
                openHigh.setName("Open high incident");
                openHigh.setSeverity(Incident.IncidentSeverity.HIGH);
                openHigh.setStatus(Incident.IncidentStatus.OPEN);
                openHigh.setApplication(application);
                incidentRepository.saveAndFlush(openHigh);

                Incident openLow = new Incident();
                openLow.setName("Open low incident");
                openLow.setSeverity(Incident.IncidentSeverity.LOW);
                openLow.setStatus(Incident.IncidentStatus.OPEN);
                openLow.setApplication(application);
                incidentRepository.saveAndFlush(openLow);

                Incident resolvedHigh = new Incident();
                resolvedHigh.setName("Resolved high incident");
                resolvedHigh.setSeverity(Incident.IncidentSeverity.HIGH);
                resolvedHigh.setStatus(Incident.IncidentStatus.RESOLVED);
                resolvedHigh.setApplication(application);
                incidentRepository.saveAndFlush(resolvedHigh);

                String token = jwtService.generateToken(owner);
                String url = "/api/applications/" + application.getId() + "/incidents";

                // Filter by status.
                mockMvc.perform(get(url + "?status=OPEN")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(2));

                // Filter by severity.
                mockMvc.perform(get(url + "?severity=HIGH")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(2));

                // Apply both filters together.
                mockMvc.perform(get(url + "?status=OPEN&severity=HIGH")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(1))
                                .andExpect(jsonPath("$.content[0].name").value("Open high incident"));
        }

        @Test
        void userCannotFilterIncidentsWithInvalidStatus() throws Exception {
                User owner = new User();
                owner.setEmail("invalid-filter@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Invalid Filter Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(get("/api/applications/" + application.getId()
                                + "/incidents?status=INVALID")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void userCanSortIncidentsByCreatedAtAscending() throws Exception {
                User owner = new User();
                owner.setEmail("sortincidents@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Sort Incidents Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                LocalDateTime now = LocalDateTime.now();

                Incident olderIncident = new Incident();
                olderIncident.setName("Older incident");
                olderIncident.setSeverity(Incident.IncidentSeverity.LOW);
                olderIncident.setStatus(Incident.IncidentStatus.OPEN);
                olderIncident.setReason("First incident");
                olderIncident.setApplication(application);
                olderIncident.setCreatedAt(now.minusHours(2));
                olderIncident.setStartedAt(now.minusHours(2));
                olderIncident = incidentRepository.saveAndFlush(olderIncident);

                Incident newerIncident = new Incident();
                newerIncident.setName("Newer incident");
                newerIncident.setSeverity(Incident.IncidentSeverity.HIGH);
                newerIncident.setStatus(Incident.IncidentStatus.OPEN);
                newerIncident.setReason("Second incident");
                newerIncident.setApplication(application);
                newerIncident.setCreatedAt(now);
                newerIncident.setStartedAt(now);
                newerIncident = incidentRepository.saveAndFlush(newerIncident);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(get("/api/applications/" + application.getId() + "/incidents")
                                .param("sort", "createdAt,asc")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(2))
                                .andExpect(jsonPath("$.content[0].id").value(olderIncident.getId()))
                                .andExpect(jsonPath("$.content[1].id").value(newerIncident.getId()));
        }

        @Test
        void userCannotSortIncidentsByInvalidField() throws Exception {
                User owner = new User();
                owner.setEmail("invalidsortfield@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Invalid Sort Field Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(get("/api/applications/" + application.getId() + "/incidents")
                                .param("sort", "password,asc")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void userCannotSortWithInvalidDirection() throws Exception {
                User owner = new User();
                owner.setEmail("invalidsortdirection@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Invalid Sort Direction Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(get("/api/applications/" + application.getId() + "/incidents")
                                .param("sort", "createdAt,sideways")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void userCanFilterIncidentsByStartedDateRange() throws Exception {
                User owner = new User();
                owner.setEmail("filter-date-range@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Date Filtering Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                Incident beforeRange = new Incident();
                beforeRange.setName("Before range");
                beforeRange.setSeverity(Incident.IncidentSeverity.HIGH);
                beforeRange.setStatus(Incident.IncidentStatus.OPEN);
                beforeRange.setStartedAt(
                                java.time.LocalDateTime.of(2026, 10, 18, 11, 59, 59));
                beforeRange.setApplication(application);
                incidentRepository.saveAndFlush(beforeRange);

                Incident atStartBoundary = new Incident();
                atStartBoundary.setName("At start boundary");
                atStartBoundary.setSeverity(Incident.IncidentSeverity.HIGH);
                atStartBoundary.setStatus(Incident.IncidentStatus.OPEN);
                atStartBoundary.setStartedAt(
                                java.time.LocalDateTime.of(2026, 10, 18, 12, 0, 0));
                atStartBoundary.setApplication(application);
                incidentRepository.saveAndFlush(atStartBoundary);

                Incident insideRange = new Incident();
                insideRange.setName("Inside range");
                insideRange.setSeverity(Incident.IncidentSeverity.LOW);
                insideRange.setStatus(Incident.IncidentStatus.OPEN);
                insideRange.setStartedAt(
                                java.time.LocalDateTime.of(2026, 10, 19, 12, 0, 0));
                insideRange.setApplication(application);
                incidentRepository.saveAndFlush(insideRange);

                Incident atEndBoundary = new Incident();
                atEndBoundary.setName("At end boundary");
                atEndBoundary.setSeverity(Incident.IncidentSeverity.HIGH);
                atEndBoundary.setStatus(Incident.IncidentStatus.RESOLVED);
                atEndBoundary.setStartedAt(
                                java.time.LocalDateTime.of(2026, 10, 20, 12, 0, 0));
                atEndBoundary.setApplication(application);
                incidentRepository.saveAndFlush(atEndBoundary);

                Incident afterRange = new Incident();
                afterRange.setName("After range");
                afterRange.setSeverity(Incident.IncidentSeverity.LOW);
                afterRange.setStatus(Incident.IncidentStatus.OPEN);
                afterRange.setStartedAt(
                                java.time.LocalDateTime.of(2026, 10, 20, 12, 0, 1));
                afterRange.setApplication(application);
                incidentRepository.saveAndFlush(afterRange);

                String token = jwtService.generateToken(owner);
                String url = "/api/applications/" + application.getId()
                                + "/incidents"
                                + "?startedAfter=2026-10-18T12:00:00"
                                + "&startedBefore=2026-10-20T12:00:00";

                mockMvc.perform(get(url)
                                .header("Authorization", "Bearer " + token))
                                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                                .andExpect(status().isOk());
        }

}
