package com.hyperskill.tracker;

import com.fasterxml.jackson.databind.JsonNode;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeveloperControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeveloperRepository developerRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        applicationRepository.deleteAll();
        developerRepository.deleteAll();
    }

    @Test
    @DisplayName("Fetching developer profile requires authentication")
    void getDeveloperProfileRequiresAuthentication() throws Exception {
        Developer developer = persistDeveloper("unauth@example.com", "password");

        mockMvc.perform(get("/api/developers/" + developer.getId()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authenticated developer cannot access another profile")
    void developerCannotAccessOtherProfile() throws Exception {
        Developer first = persistDeveloper("first@example.com", "password");
        Developer second = persistDeveloper("second@example.com", "password");

        mockMvc.perform(get("/api/developers/" + first.getId())
                .header("Authorization", basicAuthHeader(second.getEmail(), "password")))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Developer profile lists applications from newest to oldest")
    void developerProfileListsApplicationsNewestFirst() throws Exception {
        Developer developer = persistDeveloper("owner@example.com", "password");
        Application firstApp = applicationRepository.save(new Application("First", "first", "key-1", "basic", developer));
        Application secondApp = applicationRepository.save(new Application("Second", "second", "key-2", "premium", developer));

        String response = mockMvc.perform(get("/api/developers/" + developer.getId())
                .header("Authorization", basicAuthHeader(developer.getEmail(), "password"))
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(developer.getId()))
            .andExpect(jsonPath("$.email").value(developer.getEmail()))
            .andExpect(jsonPath("$.applications").isArray())
            .andExpect(jsonPath("$.applications[0].category").value("premium"))
            .andExpect(jsonPath("$.applications[1].category").value("basic"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        assertThat(root.get("applications").get(0).get("id").asLong()).isEqualTo(secondApp.getId());
        assertThat(root.get("applications").get(0).get("name").asText()).isEqualTo("Second");
        assertThat(root.get("applications").get(1).get("id").asLong()).isEqualTo(firstApp.getId());
        assertThat(root.get("applications").get(1).get("name").asText()).isEqualTo("First");
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
