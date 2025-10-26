package com.hyperskill.tracker;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FitnessTrackerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DeveloperRepository developerRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @AfterEach
    void tearDown() {
        applicationRepository.deleteAll();
        developerRepository.deleteAll();
    }

    @Test
    @DisplayName("Tracker record creation requires X-API-Key header")
    void createRecordWithoutApiKeyReturns401() throws Exception {
        TrackerRecordRequest request = new TrackerRecordRequest("user", "Run", 600, 250);

        mockMvc.perform(post("/api/tracker")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Invalid API key results in 401 Unauthorized")
    void createRecordWithInvalidApiKeyReturns401() throws Exception {
        TrackerRecordRequest request = new TrackerRecordRequest("user", "Run", 600, 250);

        mockMvc.perform(post("/api/tracker")
                .header("X-API-Key", "invalid-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Tracker listing requires X-API-Key header")
    void listRecordsWithoutApiKeyReturns401() throws Exception {
        mockMvc.perform(get("/api/tracker"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Invalid API key is rejected when listing tracker records")
    void listRecordsWithInvalidApiKeyReturns401() throws Exception {
        mockMvc.perform(get("/api/tracker")
                .header("X-API-Key", "invalid"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authenticated application can create records and application name is returned")
    void createRecordWithValidApiKeyIncludesApplication() throws Exception {
        Application application = persistApplication("Workout Tracker");

        TrackerRecordRequest request = new TrackerRecordRequest("alice", "Cycling", 1500, 600);

        mockMvc.perform(post("/api/tracker")
                .header("X-API-Key", application.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.application").value("Workout Tracker"));

        mockMvc.perform(get("/api/tracker")
                .header("X-API-Key", application.getApiKey()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].application").value("Workout Tracker"))
            .andExpect(jsonPath("$[0].username").value("alice"))
            .andExpect(jsonPath("$[0].activity").value("Cycling"))
            .andExpect(jsonPath("$[0].duration").value(1500))
            .andExpect(jsonPath("$[0].calories").value(600));
    }

    private Application persistApplication(String name) {
        Developer developer = new Developer(name.toLowerCase() + "@example.com", passwordEncoder.encode("password"));
        Developer savedDeveloper = developerRepository.save(developer);

        String apiKey = UUID.randomUUID().toString();
        Application application = new Application(name, "description", apiKey, savedDeveloper);
        return applicationRepository.save(application);
    }
}
