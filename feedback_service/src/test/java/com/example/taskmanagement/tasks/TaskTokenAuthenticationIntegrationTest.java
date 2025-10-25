package com.example.taskmanagement.tasks;

import com.example.taskmanagement.accounts.AccountRegistrationRequest;
import com.example.taskmanagement.accounts.AccountRegistrationResponse;
import com.example.taskmanagement.auth.AccountUserRepository;
import com.example.taskmanagement.auth.TokenResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskTokenAuthenticationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountUserRepository accountUserRepository;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
        accountUserRepository.deleteAll();
    }

    @Test
    void bearerToken_allowsCreatingAndFetchingTasks() throws Exception {
        String email = "user1@mail.com";
        String password = "secret1";

        registerAccount(email, password);

        String token = obtainToken(email, password);
        assertThat(token).isNotBlank();

        TaskRequest createRequest = new TaskRequest("new task", "a task for anyone");

        HttpHeaders createHeaders = new HttpHeaders();
        createHeaders.setContentType(MediaType.APPLICATION_JSON);
        createHeaders.setBearerAuth(token);

        ResponseEntity<TaskResponse> createResponse = restTemplate.exchange(
                "/api/tasks",
                HttpMethod.POST,
                new HttpEntity<>(createRequest, createHeaders),
                TaskResponse.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TaskResponse createdTask = createResponse.getBody();
        assertThat(createdTask).isNotNull();
        assertThat(createdTask.id()).isNotBlank();
        assertThat(createdTask.author()).isEqualTo(email);

        HttpHeaders listHeaders = new HttpHeaders();
        listHeaders.setBearerAuth(token);

        ResponseEntity<String> listResponse = restTemplate.exchange(
                "/api/tasks",
                HttpMethod.GET,
                new HttpEntity<>(listHeaders),
                String.class
        );

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        String listBody = listResponse.getBody();
        assertThat(listBody).isNotNull();

        List<Map<String, Object>> tasks = objectMapper.readValue(
                listBody,
                new TypeReference<>() {}
        );

        assertThat(tasks).hasSize(1);
        Map<String, Object> firstTask = tasks.get(0);
        assertThat(firstTask.get("author")).isEqualTo(email);
        assertThat(firstTask.get("title")).isEqualTo("new task");

        ResponseEntity<String> filteredResponse = restTemplate.exchange(
                "/api/tasks?author=" + email.toUpperCase(),
                HttpMethod.GET,
                new HttpEntity<>(listHeaders),
                String.class
        );

        assertThat(filteredResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String filteredBody = filteredResponse.getBody();
        assertThat(filteredBody).isNotNull();

        List<Map<String, Object>> filteredTasks = objectMapper.readValue(
                filteredBody,
                new TypeReference<>() {}
        );
        assertThat(filteredTasks).hasSize(1);
        assertThat(filteredTasks.get(0).get("author")).isEqualTo(email);

        HttpHeaders invalidHeaders = new HttpHeaders();
        invalidHeaders.setBearerAuth(token + "-invalid");

        ResponseEntity<String> unauthorizedResponse = restTemplate.exchange(
                "/api/tasks",
                HttpMethod.GET,
                new HttpEntity<>(invalidHeaders),
                String.class
        );

        assertThat(unauthorizedResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private void registerAccount(String email, String password) {
        AccountRegistrationRequest request = new AccountRegistrationRequest(email, password);
        ResponseEntity<AccountRegistrationResponse> response = restTemplate.postForEntity(
                "/api/accounts",
                request,
                AccountRegistrationResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo(email);
    }

    private String obtainToken(String email, String password) {
        ResponseEntity<TokenResponse> response = restTemplate
                .withBasicAuth(email, password)
                .exchange(
                        "/api/auth/token",
                        HttpMethod.POST,
                        new HttpEntity<>(HttpHeaders.EMPTY),
                        TokenResponse.class
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TokenResponse body = response.getBody();
        assertThat(body).isNotNull();
        return body.token();
    }
}
