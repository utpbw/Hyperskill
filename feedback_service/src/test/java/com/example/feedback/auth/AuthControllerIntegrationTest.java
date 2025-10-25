package com.example.feedback.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

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
}
