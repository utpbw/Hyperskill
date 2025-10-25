package com.example.feedback.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void signup_withValidRequest_returnsCreatedUserDetails() {
        SignupRequest request = new SignupRequest(
                "John",
                "Doe",
                "john.doe@acme.com",
                "password123"
        );

        ResponseEntity<SignupResponse> response = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                SignupResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        SignupResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.name()).isEqualTo("John");
        assertThat(body.lastname()).isEqualTo("Doe");
        assertThat(body.email()).isEqualTo("john.doe@acme.com");
    }

    @Test
    void signup_withNonCorporateEmail_returnsBadRequest() {
        SignupRequest request = new SignupRequest(
                "John",
                "Doe",
                "john.doe@gmail.com",
                "password123"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void signup_withBlankFields_returnsBadRequest() {
        SignupRequest request = new SignupRequest(
                " ",
                "",
                "",
                ""
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void signup_withDuplicateEmail_returnsBadRequest() {
        SignupRequest request = new SignupRequest(
                "Jane",
                "Doe",
                "jane.doe@acme.com",
                "password123"
        );

        ResponseEntity<SignupResponse> firstResponse = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                SignupResponse.class
        );

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> secondResponse = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                String.class
        );

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(secondResponse.getBody()).isNotNull().contains("User exist!");
    }

    @Test
    void payment_withAuthentication_returnsCurrentUserDetails() {
        SignupRequest request = new SignupRequest(
                "Alice",
                "Smith",
                "alice.smith@acme.com",
                "strongpassword"
        );

        ResponseEntity<SignupResponse> signupResponse = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                SignupResponse.class
        );

        assertThat(signupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        SignupResponse createdUser = signupResponse.getBody();
        assertThat(createdUser).isNotNull();

        ResponseEntity<SignupResponse> paymentResponse = restTemplate
                .withBasicAuth("alice.smith@acme.com", "strongpassword")
                .getForEntity(
                        "/api/empl/payment/",
                        SignupResponse.class
                );

        assertThat(paymentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        SignupResponse paymentBody = paymentResponse.getBody();
        assertThat(paymentBody).isNotNull();
        assertThat(paymentBody.id()).isEqualTo(createdUser.id());
        assertThat(paymentBody.name()).isEqualTo("Alice");
        assertThat(paymentBody.lastname()).isEqualTo("Smith");
        assertThat(paymentBody.email()).isEqualTo("alice.smith@acme.com");
    }

    @Test
    void payment_withDifferentEmailCase_authenticatesSuccessfully() {
        SignupRequest request = new SignupRequest(
                "Bob",
                "Stone",
                "bob.stone@acme.com",
                "topsecret"
        );

        ResponseEntity<SignupResponse> signupResponse = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                SignupResponse.class
        );

        assertThat(signupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<SignupResponse> paymentResponse = restTemplate
                .withBasicAuth("Bob.Stone@acme.com", "topsecret")
                .getForEntity(
                        "/api/empl/payment/",
                        SignupResponse.class
                );

        assertThat(paymentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        SignupResponse body = paymentResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(body.email()).isEqualTo("bob.stone@acme.com");
    }

    @Test
    void payment_withWrongPassword_returnsUnauthorizedErrorBody() throws Exception {
        SignupRequest request = new SignupRequest(
                "Carol",
                "Mills",
                "carol.mills@acme.com",
                "pass1234"
        );

        ResponseEntity<SignupResponse> signupResponse = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                SignupResponse.class
        );

        assertThat(signupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> paymentResponse = restTemplate
                .withBasicAuth("carol.mills@acme.com", "wrongpass")
                .getForEntity(
                        "/api/empl/payment/",
                        String.class
                );

        assertThat(paymentResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Map<?, ?> body = objectMapper.readValue(paymentResponse.getBody(), Map.class);
        assertThat(body).containsEntry("status", 401);
        assertThat(body).containsEntry("error", "Unauthorized");
        assertThat(body).containsEntry("message", "");
        assertThat(body.get("path")).isEqualTo("/api/empl/payment/");
    }

    @Test
    void payment_withoutAuthentication_returnsUnauthorized() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/empl/payment/",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Map<?, ?> body = objectMapper.readValue(response.getBody(), Map.class);
        assertThat(body).containsEntry("status", 401);
        assertThat(body).containsEntry("error", "Unauthorized");
        assertThat(body).containsEntry("message", "");
        assertThat(body.get("path")).isEqualTo("/api/empl/payment/");
    }
}
