package com.example.accounts.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driverClassName=org.h2.Driver"
})
@Transactional
class TaskCommentsIntegrationTest {

    private static final String PASSWORD = "password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskCommentRepository taskCommentRepository;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AccountEntity authorOne;
    private AccountEntity authorTwo;
    private AccountEntity commenter;

    @BeforeEach
    void setUp() {
        taskCommentRepository.deleteAll();
        taskRepository.deleteAll();
        tokenRepository.deleteAll();
        accountRepository.deleteAll();

        authorOne = persistAccount("user1@mail.com");
        authorTwo = persistAccount("user2@mail.com");
        commenter = persistAccount("user3@gmail.com");
    }

    @Test
    void postComment_byAuthenticatedUser_returnsCreatedComment() throws Exception {
        TaskEntity task = persistTask(authorOne, "new task", "a task for anyone", "CREATED", null);

        String token = obtainToken(commenter.getEmail());

        mockMvc.perform(post("/api/tasks/" + task.getId() + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"I'll be happy to take it!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.task_id").value(String.valueOf(task.getId())))
                .andExpect(jsonPath("$.text").value("I'll be happy to take it!"))
                .andExpect(jsonPath("$.author").value(commenter.getNormalizedEmail()));

        assertThat(taskCommentRepository.count()).isEqualTo(1);
    }

    @Test
    void postComment_withBlankText_returnsBadRequest() throws Exception {
        TaskEntity task = persistTask(authorOne, "new task", "a task for anyone", "CREATED", null);
        String token = obtainToken(commenter.getEmail());

        mockMvc.perform(post("/api/tasks/" + task.getId() + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"\"}"))
                .andExpect(status().isBadRequest());

        assertThat(taskCommentRepository.count()).isZero();
    }

    @Test
    void postComment_toUnknownTask_returnsNotFound() throws Exception {
        String token = obtainToken(commenter.getEmail());

        mockMvc.perform(post("/api/tasks/300/comments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"I'll be happy to take it!\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTasks_returnsNewestTasksWithCommentTotals() throws Exception {
        TaskEntity firstTask = persistTask(authorOne, "new task", "a task for anyone", "COMPLETED", authorTwo);
        TaskEntity secondTask = persistTask(authorTwo, "second task", "another task", "CREATED", null);

        persistComment(firstTask, commenter, "I'll be happy to take it!");

        String token = obtainToken(authorOne.getEmail());

        mockMvc.perform(get("/api/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(String.valueOf(secondTask.getId())))
                .andExpect(jsonPath("$[0].title").value("second task"))
                .andExpect(jsonPath("$[0].description").value("another task"))
                .andExpect(jsonPath("$[0].status").value("CREATED"))
                .andExpect(jsonPath("$[0].author").value(authorTwo.getNormalizedEmail()))
                .andExpect(jsonPath("$[0].assignee").value("none"))
                .andExpect(jsonPath("$[0].total_comments").value(0))
                .andExpect(jsonPath("$[1].id").value(String.valueOf(firstTask.getId())))
                .andExpect(jsonPath("$[1].title").value("new task"))
                .andExpect(jsonPath("$[1].description").value("a task for anyone"))
                .andExpect(jsonPath("$[1].status").value("COMPLETED"))
                .andExpect(jsonPath("$[1].author").value(authorOne.getNormalizedEmail()))
                .andExpect(jsonPath("$[1].assignee").value(authorTwo.getNormalizedEmail()))
                .andExpect(jsonPath("$[1].total_comments").value(1));
    }

    @Test
    void getComments_returnsNewestCommentsFirst() throws Exception {
        TaskEntity task = persistTask(authorOne, "new task", "a task for anyone", "COMPLETED", authorTwo);
        TaskCommentEntity comment = persistComment(task, commenter, "I'll be happy to take it!");

        String token = obtainToken(authorOne.getEmail());

        mockMvc.perform(get("/api/tasks/" + task.getId() + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(String.valueOf(comment.getId())))
                .andExpect(jsonPath("$[0].task_id").value(String.valueOf(task.getId())))
                .andExpect(jsonPath("$[0].text").value("I'll be happy to take it!"))
                .andExpect(jsonPath("$[0].author").value(commenter.getNormalizedEmail()));
    }

    private AccountEntity persistAccount(String email) {
        AccountEntity entity = new AccountEntity();
        entity.setEmail(email);
        entity.setNormalizedEmail(email.toLowerCase(Locale.ROOT));
        entity.setPassword(passwordEncoder.encode(PASSWORD));
        return accountRepository.save(entity);
    }

    private TaskEntity persistTask(AccountEntity author, String title, String description, String status, AccountEntity assignee) {
        TaskEntity entity = new TaskEntity();
        entity.setTitle(title);
        entity.setDescription(description);
        entity.setStatus(status);
        entity.setAuthorEmail(author.getNormalizedEmail());
        entity.setAssigneeEmail(assignee == null ? null : assignee.getNormalizedEmail());
        return taskRepository.save(entity);
    }

    private TaskCommentEntity persistComment(TaskEntity task, AccountEntity commentAuthor, String text) {
        TaskCommentEntity entity = new TaskCommentEntity();
        entity.setTask(task);
        entity.setAuthorEmail(commentAuthor.getNormalizedEmail());
        entity.setText(text);
        return taskCommentRepository.save(entity);
    }

    private String obtainToken(String email) throws Exception {
        byte[] response = mockMvc.perform(post("/api/auth/token")
                        .with(httpBasic(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        JsonNode root = objectMapper.readTree(new String(response, StandardCharsets.UTF_8));
        return root.get("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
