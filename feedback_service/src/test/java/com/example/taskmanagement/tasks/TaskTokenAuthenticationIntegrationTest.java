package com.example.taskmanagement.tasks;

import com.example.taskmanagement.accounts.AccountRegistrationRequest;
import com.example.taskmanagement.accounts.AccountRegistrationResponse;
import com.example.taskmanagement.auth.AccountUserRepository;
import com.example.taskmanagement.auth.TokenResponse;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskTokenAuthenticationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountUserRepository accountUserRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCommentRepository taskCommentRepository;

    @BeforeEach
    void cleanDatabase() {
        taskCommentRepository.deleteAll();
        taskRepository.deleteAll();
        accountUserRepository.deleteAll();
    }

    @Test
    void bearerToken_allowsTaskLifecycleOperations() {
        String authorEmail = "user1@mail.com";
        String authorPassword = "secret1";
        String assigneeEmail = "user2@mail.com";
        String assigneePassword = "secret2";

        registerAccount(authorEmail, authorPassword);
        registerAccount(assigneeEmail, assigneePassword);

        String authorToken = obtainToken(authorEmail, authorPassword);
        String assigneeToken = obtainToken(assigneeEmail, assigneePassword);

        TaskRequest createRequest = new TaskRequest("new task", "a task for anyone");

        HttpHeaders createHeaders = new HttpHeaders();
        createHeaders.setContentType(MediaType.APPLICATION_JSON);
        createHeaders.setBearerAuth(authorToken);

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
        assertThat(createdTask.author()).isEqualTo(authorEmail);
        assertThat(createdTask.assignee()).isEqualTo("none");
        assertThat(createdTask.totalComments()).isZero();

        long taskId = Long.parseLong(createdTask.id());

        TaskAssignmentRequest assignRequest = new TaskAssignmentRequest(assigneeEmail);
        HttpHeaders assignHeaders = new HttpHeaders();
        assignHeaders.setContentType(MediaType.APPLICATION_JSON);
        assignHeaders.setBearerAuth(authorToken);

        ResponseEntity<TaskResponse> assignResponse = restTemplate.exchange(
                "/api/tasks/" + taskId + "/assign",
                HttpMethod.PUT,
                new HttpEntity<>(assignRequest, assignHeaders),
                TaskResponse.class
        );

        assertThat(assignResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TaskResponse assignedTask = assignResponse.getBody();
        assertThat(assignedTask).isNotNull();
        assertThat(assignedTask.assignee()).isEqualTo(assigneeEmail);
        assertThat(assignedTask.totalComments()).isZero();

        TaskCommentRequest commentRequest = new TaskCommentRequest("Great job");
        HttpHeaders commentHeaders = new HttpHeaders();
        commentHeaders.setContentType(MediaType.APPLICATION_JSON);
        commentHeaders.setBearerAuth(assigneeToken);

        ResponseEntity<TaskCommentResponse> commentResponse = restTemplate.exchange(
                "/api/tasks/" + taskId + "/comments",
                HttpMethod.POST,
                new HttpEntity<>(commentRequest, commentHeaders),
                TaskCommentResponse.class
        );

        assertThat(commentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TaskCommentResponse postedComment = commentResponse.getBody();
        assertThat(postedComment).isNotNull();
        assertThat(postedComment.taskId()).isEqualTo(String.valueOf(taskId));
        assertThat(postedComment.author()).isEqualTo(assigneeEmail);
        assertThat(postedComment.text()).isEqualTo("Great job");

        TaskCommentRequest secondCommentRequest = new TaskCommentRequest("Needs review");
        HttpHeaders secondCommentHeaders = new HttpHeaders();
        secondCommentHeaders.setContentType(MediaType.APPLICATION_JSON);
        secondCommentHeaders.setBearerAuth(authorToken);

        ResponseEntity<TaskCommentResponse> secondCommentResponse = restTemplate.exchange(
                "/api/tasks/" + taskId + "/comments",
                HttpMethod.POST,
                new HttpEntity<>(secondCommentRequest, secondCommentHeaders),
                TaskCommentResponse.class
        );

        assertThat(secondCommentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TaskCommentResponse authorComment = secondCommentResponse.getBody();
        assertThat(authorComment).isNotNull();

        HttpHeaders listCommentsHeaders = new HttpHeaders();
        listCommentsHeaders.setBearerAuth(authorToken);

        ResponseEntity<TaskCommentResponse[]> commentListResponse = restTemplate.exchange(
                "/api/tasks/" + taskId + "/comments",
                HttpMethod.GET,
                new HttpEntity<>(listCommentsHeaders),
                TaskCommentResponse[].class
        );

        assertThat(commentListResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TaskCommentResponse[] comments = commentListResponse.getBody();
        assertThat(comments).isNotNull();
        assertThat(comments).hasSize(2);
        assertThat(comments[0].id()).isEqualTo(authorComment.id());
        assertThat(comments[1].id()).isEqualTo(postedComment.id());

        ResponseEntity<String> unauthorizedCommentsResponse = restTemplate.exchange(
                "/api/tasks/" + taskId + "/comments",
                HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()),
                String.class
        );

        assertThat(unauthorizedCommentsResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        TaskCommentRequest invalidCommentRequest = new TaskCommentRequest("   ");
        ResponseEntity<String> invalidCommentResponse = restTemplate.exchange(
                "/api/tasks/" + taskId + "/comments",
                HttpMethod.POST,
                new HttpEntity<>(invalidCommentRequest, commentHeaders),
                String.class
        );

        assertThat(invalidCommentResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        HttpHeaders unauthorizedAssignHeaders = new HttpHeaders();
        unauthorizedAssignHeaders.setContentType(MediaType.APPLICATION_JSON);
        unauthorizedAssignHeaders.setBearerAuth(assigneeToken);

        ResponseEntity<TaskResponse> forbiddenAssign = restTemplate.exchange(
                "/api/tasks/" + taskId + "/assign",
                HttpMethod.PUT,
                new HttpEntity<>(assignRequest, unauthorizedAssignHeaders),
                TaskResponse.class
        );

        assertThat(forbiddenAssign.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        TaskStatusUpdateRequest inProgressRequest = new TaskStatusUpdateRequest(TaskStatus.IN_PROGRESS);
        HttpHeaders statusHeaders = new HttpHeaders();
        statusHeaders.setContentType(MediaType.APPLICATION_JSON);
        statusHeaders.setBearerAuth(assigneeToken);

        ResponseEntity<TaskResponse> inProgressResponse = restTemplate.exchange(
                "/api/tasks/" + taskId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(inProgressRequest, statusHeaders),
                TaskResponse.class
        );

        assertThat(inProgressResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TaskResponse inProgressTask = inProgressResponse.getBody();
        assertThat(inProgressTask).isNotNull();
        assertThat(inProgressTask.status()).isEqualTo(TaskStatus.IN_PROGRESS.name());
        assertThat(inProgressTask.totalComments()).isEqualTo(2);

        TaskStatusUpdateRequest completedRequest = new TaskStatusUpdateRequest(TaskStatus.COMPLETED);
        statusHeaders.setBearerAuth(authorToken);

        ResponseEntity<TaskResponse> completedResponse = restTemplate.exchange(
                "/api/tasks/" + taskId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(completedRequest, statusHeaders),
                TaskResponse.class
        );

        assertThat(completedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TaskResponse completedTask = completedResponse.getBody();
        assertThat(completedTask).isNotNull();
        assertThat(completedTask.status()).isEqualTo(TaskStatus.COMPLETED.name());
        assertThat(completedTask.totalComments()).isEqualTo(2);

        ResponseEntity<TaskResponse[]> assignedFilterResponse = restTemplate.exchange(
                "/api/tasks?assignee=" + assigneeEmail.toUpperCase(),
                HttpMethod.GET,
                new HttpEntity<>(statusHeaders),
                TaskResponse[].class
        );

        assertThat(assignedFilterResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TaskResponse[] assignedFiltered = assignedFilterResponse.getBody();
        assertThat(assignedFiltered).isNotNull();
        assertThat(assignedFiltered).hasSize(1);
        assertThat(assignedFiltered[0].totalComments()).isEqualTo(2);

        TaskAssignmentRequest unassignRequest = new TaskAssignmentRequest("none");
        ResponseEntity<TaskResponse> unassignResponse = restTemplate.exchange(
                "/api/tasks/" + taskId + "/assign",
                HttpMethod.PUT,
                new HttpEntity<>(unassignRequest, assignHeaders),
                TaskResponse.class
        );

        assertThat(unassignResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TaskResponse unassignedTask = unassignResponse.getBody();
        assertThat(unassignedTask).isNotNull();
        assertThat(unassignedTask.assignee()).isEqualTo("none");
        assertThat(unassignedTask.totalComments()).isEqualTo(2);

        HttpHeaders unassignedFilterHeaders = new HttpHeaders();
        unassignedFilterHeaders.setBearerAuth(authorToken);

        ResponseEntity<TaskResponse[]> unassignedFilterResponse = restTemplate.exchange(
                "/api/tasks?assignee=none",
                HttpMethod.GET,
                new HttpEntity<>(unassignedFilterHeaders),
                TaskResponse[].class
        );

        assertThat(unassignedFilterResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TaskResponse[] unassignedFiltered = unassignedFilterResponse.getBody();
        assertThat(unassignedFiltered).isNotNull();
        assertThat(unassignedFiltered).hasSize(1);
        assertThat(unassignedFiltered[0].assignee()).isEqualTo("none");
        assertThat(unassignedFiltered[0].totalComments()).isEqualTo(2);

        HttpHeaders listHeaders = new HttpHeaders();
        listHeaders.setBearerAuth(authorToken);

        ResponseEntity<TaskResponse[]> listResponse = restTemplate.exchange(
                "/api/tasks",
                HttpMethod.GET,
                new HttpEntity<>(listHeaders),
                TaskResponse[].class
        );

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TaskResponse[] allTasks = listResponse.getBody();
        assertThat(allTasks).isNotNull();
        assertThat(allTasks).hasSize(1);
        assertThat(allTasks[0].assignee()).isEqualTo("none");
        assertThat(allTasks[0].totalComments()).isEqualTo(2);

        ResponseEntity<TaskResponse[]> authorFilterResponse = restTemplate.exchange(
                "/api/tasks?author=" + authorEmail.toUpperCase(),
                HttpMethod.GET,
                new HttpEntity<>(listHeaders),
                TaskResponse[].class
        );

        assertThat(authorFilterResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TaskResponse[] authorFiltered = authorFilterResponse.getBody();
        assertThat(authorFiltered).isNotNull();
        assertThat(authorFiltered).hasSize(1);
        assertThat(authorFiltered[0].totalComments()).isEqualTo(2);

        ResponseEntity<TaskResponse[]> assigneeFilterResponse = restTemplate.exchange(
                "/api/tasks?assignee=" + assigneeEmail.toUpperCase(),
                HttpMethod.GET,
                new HttpEntity<>(listHeaders),
                TaskResponse[].class
        );

        assertThat(assigneeFilterResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TaskResponse[] assigneeFiltered = assigneeFilterResponse.getBody();
        assertThat(assigneeFiltered).isNotNull();
        assertThat(assigneeFiltered).isEmpty();

        HttpHeaders invalidHeaders = new HttpHeaders();
        invalidHeaders.setBearerAuth(authorToken + "-invalid");

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
