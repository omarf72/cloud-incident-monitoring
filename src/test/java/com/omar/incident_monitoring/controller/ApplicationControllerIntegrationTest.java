package com.omar.incident_monitoring.controller;

import com.omar.incident_monitoring.entity.Application;
import com.omar.incident_monitoring.entity.User;
import com.omar.incident_monitoring.repository.ApplicationRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

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

}
