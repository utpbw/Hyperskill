package com.hyperskill.tracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeveloperRepository developerRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        applicationRepository.deleteAll();
        developerRepository.deleteAll();
    }

    @Test
    @DisplayName("Registering an application requires authentication")
    void registerApplicationRequiresAuthentication() throws Exception {
        ApplicationRegistrationRequest request = new ApplicationRegistrationRequest("My App", "description");

        mockMvc.perform(post("/api/applications/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Invalid application payload results in 400 Bad Request")
    void registerApplicationWithInvalidBodyReturns400() throws Exception {
        Developer developer = persistDeveloper("dev@example.com", "password");

        ApplicationRegistrationRequest request = new ApplicationRegistrationRequest(" ", "description");

        mockMvc.perform(post("/api/applications/register")
                .header("Authorization", basicAuthHeader(developer.getEmail(), "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Successful registration returns API key and persists the application")
    void registerApplicationSuccessReturnsApiKey() throws Exception {
        Developer developer = persistDeveloper("owner@example.com", "password");

        ApplicationRegistrationRequest request = new ApplicationRegistrationRequest("Workout App", "Tracks workouts");

        String response = mockMvc.perform(post("/api/applications/register")
                .header("Authorization", basicAuthHeader(developer.getEmail(), "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Workout App"))
            .andExpect(jsonPath("$.apikey").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

        ApplicationRegistrationResponse registrationResponse =
            objectMapper.readValue(response, ApplicationRegistrationResponse.class);

        assertThat(applicationRepository.existsByName("Workout App")).isTrue();
        assertThat(applicationRepository.existsByApiKey(registrationResponse.apikey())).isTrue();
    }

    @Test
    @DisplayName("Duplicate application name returns 400 Bad Request")
    void registerApplicationWithDuplicateNameReturns400() throws Exception {
        Developer developer = persistDeveloper("duplicate@example.com", "password");

        ApplicationRegistrationRequest request = new ApplicationRegistrationRequest("Tracker", "desc");

        mockMvc.perform(post("/api/applications/register")
                .header("Authorization", basicAuthHeader(developer.getEmail(), "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/applications/register")
                .header("Authorization", basicAuthHeader(developer.getEmail(), "password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    private Developer persistDeveloper(String email, String rawPassword) {
        Developer developer = new Developer(email, passwordEncoder.encode(rawPassword));
        return developerRepository.save(developer);
    }

    private String basicAuthHeader(String username, String password) {
        String token = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}

