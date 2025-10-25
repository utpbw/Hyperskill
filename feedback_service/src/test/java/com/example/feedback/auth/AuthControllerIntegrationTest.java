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
    void payment_withoutAuthentication_returnsUnauthorized() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/empl/payment/",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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
}
