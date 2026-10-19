package com.omar.incident_monitoring.controller;

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
class ApplicationControllerIntegrationTest {

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
        void userCannotAccessAnotherUsersApplication() throws Exception {

                // Create the owner of the application.
                User owner = new User();
                owner.setEmail("owner-test@example.com");
                owner.setPasswordHash("test-password");
                owner = userRepository.saveAndFlush(owner);

                // Create a different user who will attempt to access it.
                User otherUser = new User();
                otherUser.setEmail("other-test@example.com");
                otherUser.setPasswordHash("test-password");
                otherUser = userRepository.saveAndFlush(otherUser);

                // Create an application owned by the first user.
                Application application = new Application();
                application.setName("Owner's Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                // Authenticate as the other user.
                String token = jwtService.generateToken(otherUser);

                // The other user should not be able to retrieve this application.
                mockMvc.perform(get("/api/applications/" + application.getId())
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound());
        }

        @Test
        void unauthenticatedUserCannotAccessApplications() throws Exception {
                mockMvc.perform(get("/api/applications"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void userCanAccessTheirOwnApplication() throws Exception {
                User owner = new User();
                owner.setEmail("own-app-test@example.com");
                owner.setPasswordHash("test-password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("My Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(get("/api/applications/" + application.getId())
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk());
        }

        @Test
        void applicationCreationRejectsBlankName() throws Exception {
                User user = new User();
                user.setEmail("invalid-app-test@example.com");
                user.setPasswordHash("test-password");
                user = userRepository.saveAndFlush(user);

                String token = jwtService.generateToken(user);

                mockMvc.perform(post("/api/applications")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {"name":"   "}
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void userCreateServiceUnderOwnerApplication() throws Exception {
                User owner = new User();
                owner.setEmail("own-app@example.com");
                owner.setPasswordHash("test-password");
                owner = userRepository.saveAndFlush(owner);

                String token = jwtService.generateToken(owner);

                Application application = new Application();
                application.setName("My Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                mockMvc.perform(post("/api/applications/" + application.getId() + "/services")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Payment API",
                                                    "type": "API"
                                                }
                                                """))
                                .andDo(print())
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value("Payment API"))
                                .andExpect(jsonPath("$.type").value("API"));

        }

        @Test
        void userCanListServicesUnderTheirApplication() throws Exception {
                // Create the owner.
                User owner = new User();
                owner.setEmail("list-services-test@example.com");
                owner.setPasswordHash("test-password");
                owner = userRepository.saveAndFlush(owner);

                // Create the application.
                Application application = new Application();
                application.setName("Monitoring Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                // Create the first service.
                Service service1 = new Service();
                service1.setName("Payment API");
                service1.setType("API");
                service1.setApplication(application);

                // Create the second service.
                Service service2 = new Service();
                service2.setName("User Database");
                service2.setType("DATABASE");
                service2.setApplication(application);

                serviceRepository.saveAndFlush(service1);
                serviceRepository.saveAndFlush(service2);

                // Authenticate as the owner.
                String token = jwtService.generateToken(owner);

                // Request the services belonging to the application.
                mockMvc.perform(get("/api/applications/" + application.getId() + "/services")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[*].name").value(
                                                org.hamcrest.Matchers.containsInAnyOrder(
                                                                "Payment API", "User Database")))
                                .andExpect(jsonPath("$[*].type").value(
                                                org.hamcrest.Matchers.containsInAnyOrder(
                                                                "API", "DATABASE")));
        }

        @Test
        void cantListServicesUnderOverOwnership() throws Exception {
                User owner = new User();
                owner.setEmail("owner@exmaple.com");
                owner.setPasswordHash("password");

                owner = userRepository.saveAndFlush(owner);

                User user = new User();
                user.setEmail("user@exmaple.com");
                user.setPasswordHash("password");

                user = userRepository.saveAndFlush(user);

                Application application = new Application();

                application.setName("Google");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                // Create the first service.
                Service service = new Service();
                service.setName("Payment API");
                service.setType("API");
                service.setApplication(application);
                service = serviceRepository.saveAndFlush(service);

                String token = jwtService.generateToken(user);

                mockMvc.perform(get("/api/applications/" + application.getId() + "/services")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound());
        }

        @Test
        void listEmptySerivceList() throws Exception {
                User owner = new User();
                owner.setEmail("owner2@exmaple.com");
                owner.setPasswordHash("password");

                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();

                application.setName("Google");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(get("/api/applications/" + application.getId() + "/services")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(0)));

        }

        @Test
        void userCanUpdateServiceUnderTheirApplication() throws Exception {
                // Create the owner.
                User owner = new User();
                owner.setEmail("update-service-test@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                // Create the application.
                Application application = new Application();
                application.setName("Update Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                // Create the service to update.
                Service service = new Service();
                service.setName("Payment API");
                service.setType("API");
                service.setApplication(application);
                service = serviceRepository.saveAndFlush(service);

                String token = jwtService.generateToken(owner);

                // Update the service.
                mockMvc.perform(put("/api/applications/" + application.getId()
                                + "/services/" + service.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Payments API",
                                                    "type": "API"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Payments API"))
                                .andExpect(jsonPath("$.type").value("API"));
        }

        @Test
        void anotherUserCannotUpdateServiceUnderOtherOwnership() throws Exception {
                // Create the owner.
                User owner = new User();
                owner.setEmail("update-service@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                User user = new User();
                user.setEmail("non-owner@example.com");
                user.setPasswordHash("password");
                user = userRepository.saveAndFlush(user);

                // Create the application.
                Application application = new Application();
                application.setName("Update Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                // Create the service to update.
                Service service = new Service();
                service.setName("Payment API");
                service.setType("API");
                service.setApplication(application);
                service = serviceRepository.saveAndFlush(service);

                String token = jwtService.generateToken(user);

                // Update the service.
                mockMvc.perform(put("/api/applications/" + application.getId()
                                + "/services/" + service.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Payments API",
                                                    "type": "API"
                                                }
                                                """))
                                .andExpect(status().isNotFound());
        }

        @Test
        void userCannotUpdateNonexistentService() throws Exception {
                User owner = new User();
                owner.setEmail("none-service@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Update Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(put("/api/applications/" + application.getId()
                                + "/services/999999")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Payments API",
                                                    "type": "API"
                                                }
                                                """))
                                .andExpect(status().isNotFound());

        }

        @Test
        void userCannotUpdateServiceWithBlankName() throws Exception {
                User owner = new User();
                owner.setEmail("blank-name-update@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Validation Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                Service service = new Service();
                service.setName("Payment API");
                service.setType("API");
                service.setApplication(application);
                service = serviceRepository.saveAndFlush(service);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(put("/api/applications/" + application.getId()
                                + "/services/" + service.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "   ",
                                                    "type": "API"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void userCannotUpdateServiceWithBlankType() throws Exception {
                User owner = new User();
                owner.setEmail("user-cant-update-blank-type@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Validation Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                Service service = new Service();
                service.setName("Payment API");
                service.setType("API");
                service.setApplication(application);
                service = serviceRepository.saveAndFlush(service);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(put("/api/applications/" + application.getId()
                                + "/services/" + service.getId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "name": "Paymet API",
                                                    "type": " "
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void ownerDeleteService() throws Exception {
                User owner = new User();
                owner.setEmail("delete-service@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Validation Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                Service service = new Service();
                service.setName("Payment API");
                service.setType("API");
                service.setApplication(application);
                service = serviceRepository.saveAndFlush(service);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(delete("/api/applications/" + application.getId()
                                + "/services/" + service.getId())
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNoContent());

                assertFalse(serviceRepository.existsById(service.getId()));
        }

        @Test
        void userCannotDeleteAnotherOwnerService() throws Exception {
                User owner = new User();
                owner.setEmail("usercantdelete-another-service@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                User user = new User();
                user.setEmail("other-user@example.com");
                user.setPasswordHash("password");
                user = userRepository.saveAndFlush(user);

                Application application = new Application();
                application.setName("Authorization Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                Service service = new Service();
                service.setName("Payment API");
                service.setType("API");
                service.setApplication(application);
                service = serviceRepository.saveAndFlush(service);

                String token = jwtService.generateToken(user);

                mockMvc.perform(delete("/api/applications/" + application.getId()
                                + "/services/" + service.getId())
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound());
        }

        @Test
        void ownerDeleteNonexistentService() throws Exception {
                User owner = new User();
                owner.setEmail("delet-none-existent-service@example.com");
                owner.setPasswordHash("password");
                owner = userRepository.saveAndFlush(owner);

                Application application = new Application();
                application.setName("Validation Test Application");
                application.setUser(owner);
                application = applicationRepository.saveAndFlush(application);

                String token = jwtService.generateToken(owner);

                mockMvc.perform(delete("/api/applications/" + application.getId()
                                + "/services/9999")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound());
        }

        

}
