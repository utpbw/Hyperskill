package com.hyperskill.tracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ExampleScenariosTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Provided documentation examples execute successfully")
    void exampleFlowBehavesAsDocumented() throws Exception {
        DeveloperRequest signupRequest = new DeveloperRequest("johndoe@gmail.com", "qwerty");

        MvcResult signupResult = mockMvc.perform(post("/api/developers/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        String location = signupResult.getResponse().getHeader("Location");
        assertThat(location).isNotNull();
        assertThat(location).matches("/api/developers/\\d+");

        ApplicationRegistrationRequest registrationRequest =
            new ApplicationRegistrationRequest("Fitness App", "demo application", "basic");

        MvcResult registrationResult = mockMvc.perform(post("/api/applications/register")
                .header("Authorization", basicAuth("johndoe@gmail.com", "qwerty"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Fitness App"))
            .andExpect(jsonPath("$.category").value("basic"))
            .andExpect(jsonPath("$.apikey").isNotEmpty())
            .andReturn();

        ApplicationRegistrationResponse registrationResponse = objectMapper.readValue(
            registrationResult.getResponse().getContentAsString(),
            ApplicationRegistrationResponse.class
        );
        String apiKey = registrationResponse.apikey();
        assertThat(apiKey).isNotBlank();

        TrackerRecordRequest trackerRecordRequest =
            new TrackerRecordRequest("user-12", "swimming", 950, 320);

        mockMvc.perform(post("/api/tracker")
                .header("X-API-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(trackerRecordRequest)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tracker")
                .header("X-API-Key", apiKey))
            .andExpect(status().isTooManyRequests());
    }

    private String basicAuth(String username, String password) {
        String token = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}
