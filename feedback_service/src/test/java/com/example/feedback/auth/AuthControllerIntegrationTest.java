package com.example.feedback.auth;

import com.example.feedback.accounting.EmployeePayrollResponse;
import com.example.feedback.accounting.PayrollRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private Map<?, ?> parseBody(ResponseEntity<String> response) {
        try {
            return objectMapper.readValue(response.getBody(), Map.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse response body", ex);
        }
    }

    private SignupResponse registerUser(String email, String password) {
        SignupRequest request = new SignupRequest(
                "John",
                "Doe",
                email,
                password
        );

        ResponseEntity<SignupResponse> response = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                SignupResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        SignupResponse body = response.getBody();
        assertThat(body).isNotNull();
        return body;
    }

    @Test
    void signup_withValidRequest_returnsCreatedUserDetails() {
        SignupRequest request = new SignupRequest(
                "John",
                "Doe",
                "john.doe@acme.com",
                "password12345"
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
                "password12345"
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
                "password12345"
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
    void signup_withShortPassword_returnsBadRequestWithMessage() {
        SignupRequest request = new SignupRequest(
                "June",
                "Short",
                "june.short@acme.com",
                "shortpass"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> body = parseBody(response);
        assertThat(body.get("status")).isEqualTo(400);
        assertThat(body.get("error")).isEqualTo("Bad Request");
        assertThat(body.get("message")).isEqualTo("Password length must be 12 chars minimum!");
        assertThat(body.get("path")).isEqualTo("/api/auth/signup");
    }

    @Test
    void signup_withBreachedPassword_returnsBadRequestWithMessage() {
        SignupRequest request = new SignupRequest(
                "Breach",
                "Test",
                "breach.test@acme.com",
                "PasswordForJanuary"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> body = parseBody(response);
        assertThat(body.get("status")).isEqualTo(400);
        assertThat(body.get("error")).isEqualTo("Bad Request");
        assertThat(body.get("message")).isEqualTo("The password is in the hacker's database!");
        assertThat(body.get("path")).isEqualTo("/api/auth/signup");
    }

    @Test
    void payment_withoutRecords_returnsEmptyList() {
        registerUser("alice.smith@acme.com", "strongpassword");

        ResponseEntity<EmployeePayrollResponse[]> response = restTemplate
                .withBasicAuth("alice.smith@acme.com", "strongpassword")
                .getForEntity("/api/empl/payment", EmployeePayrollResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        EmployeePayrollResponse[] body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).isEmpty();
    }

    @Test
    void uploadPayments_withValidRequest_persistsPayroll() {
        registerUser("jane.payroll@acme.com", "verystrongpass");

        PayrollRequest payroll = new PayrollRequest("jane.payroll@acme.com", "01-2024", 1_000_00L);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/acct/payments",
                List.of(payroll),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().containsEntry("status", "Added successfully!");

        ResponseEntity<EmployeePayrollResponse[]> paymentsResponse = restTemplate
                .withBasicAuth("jane.payroll@acme.com", "verystrongpass")
                .getForEntity("/api/empl/payment", EmployeePayrollResponse[].class);

        assertThat(paymentsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        EmployeePayrollResponse[] payrolls = paymentsResponse.getBody();
        assertThat(payrolls).isNotNull();
        assertThat(payrolls).hasSize(1);
        assertThat(payrolls[0].period()).isEqualTo("January-2024");
        assertThat(payrolls[0].salary()).isEqualTo("1000 dollar(s) 0 cent(s)");
    }

    @Test
    void uploadPayments_withDuplicatePeriod_returnsBadRequest() {
        registerUser("duplicate@acme.com", "anotherstrongpass");

        PayrollRequest payroll = new PayrollRequest("duplicate@acme.com", "02-2024", 120_000L);
        ResponseEntity<Map> firstResponse = restTemplate.postForEntity(
                "/api/acct/payments",
                List.of(payroll),
                Map.class
        );

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> secondResponse = restTemplate.postForEntity(
                "/api/acct/payments",
                List.of(payroll),
                String.class
        );

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> body = parseBody(secondResponse);
        assertThat(body.get("path")).isEqualTo("/api/acct/payments");
    }

    @Test
    void uploadPayments_withUnknownEmployee_returnsBadRequest() {
        PayrollRequest payroll = new PayrollRequest("unknown@acme.com", "03-2024", 500_00L);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/acct/payments",
                List.of(payroll),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> body = parseBody(response);
        assertThat(body.get("message")).isNotNull();
    }

    @Test
    void updatePayments_withValidRequest_updatesSalary() {
        registerUser("update@acme.com", "superstrongpass");
        PayrollRequest initial = new PayrollRequest("update@acme.com", "04-2024", 100_00L);

        ResponseEntity<Map> uploadResponse = restTemplate.postForEntity(
                "/api/acct/payments",
                List.of(initial),
                Map.class
        );
        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        PayrollRequest update = new PayrollRequest("update@acme.com", "04-2024", 150_00L);
        ResponseEntity<Map> updateResponse = restTemplate.exchange(
                "/api/acct/payments",
                HttpMethod.PUT,
                new HttpEntity<>(update),
                Map.class
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).isNotNull().containsEntry("status", "Updated successfully!");

        ResponseEntity<EmployeePayrollResponse> payrollResponse = restTemplate
                .withBasicAuth("update@acme.com", "superstrongpass")
                .getForEntity("/api/empl/payment?period=04-2024", EmployeePayrollResponse.class);

        assertThat(payrollResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        EmployeePayrollResponse payroll = payrollResponse.getBody();
        assertThat(payroll).isNotNull();
        assertThat(payroll.salary()).isEqualTo("150 dollar(s) 0 cent(s)");
    }

    @Test
    void getPayments_withInvalidPeriodFormat_returnsBadRequest() {
        registerUser("invalid.period@acme.com", "yetanotherstrongpass");

        ResponseEntity<String> response = restTemplate
                .withBasicAuth("invalid.period@acme.com", "yetanotherstrongpass")
                .getForEntity("/api/empl/payment?period=2024-04", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> body = parseBody(response);
        assertThat(body.get("path")).isEqualTo("/api/empl/payment");
    }

    @Test
    void payment_withDifferentEmailCase_authenticatesSuccessfully() {
        SignupRequest request = new SignupRequest(
                "Bob",
                "Stone",
                "bob.stone@acme.com",
                "topsecretpass"
        );

        ResponseEntity<SignupResponse> signupResponse = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                SignupResponse.class
        );

        assertThat(signupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<EmployeePayrollResponse[]> paymentResponse = restTemplate
                .withBasicAuth("Bob.Stone@acme.com", "topsecretpass")
                .getForEntity(
                        "/api/empl/payment/",
                        EmployeePayrollResponse[].class
                );

        assertThat(paymentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        EmployeePayrollResponse[] body = paymentResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(body).isEmpty();
    }

    @Test
    void payment_withWrongPassword_returnsUnauthorizedErrorBody() throws Exception {
        SignupRequest request = new SignupRequest(
                "Carol",
                "Mills",
                "carol.mills@acme.com",
                "pass1234secure"
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
        assertThat(body.get("status")).isEqualTo(401);
        assertThat(body.get("error")).isEqualTo("Unauthorized");
        assertThat(body.get("message")).isEqualTo("");
        assertThat(body.get("path")).isEqualTo("/api/empl/payment/");
    }

    @Test
    void changePassword_withValidRequest_updatesPassword() {
        SignupRequest request = new SignupRequest(
                "Diane",
                "Evans",
                "diane.evans@acme.com",
                "initialPass123"
        );

        ResponseEntity<SignupResponse> signupResponse = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                SignupResponse.class
        );

        assertThat(signupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<PasswordChangeResponse> changeResponse = restTemplate
                .withBasicAuth("diane.evans@acme.com", "initialPass123")
                .postForEntity(
                        "/api/auth/changepass",
                        new PasswordChangeRequest("newSecurePass123"),
                        PasswordChangeResponse.class
                );

        assertThat(changeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        PasswordChangeResponse body = changeResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(body.email()).isEqualTo("diane.evans@acme.com");
        assertThat(body.status()).isEqualTo("The password has been updated successfully");

        ResponseEntity<String> oldPasswordResponse = restTemplate
                .withBasicAuth("diane.evans@acme.com", "initialPass123")
                .getForEntity(
                        "/api/empl/payment/",
                        String.class
                );
        assertThat(oldPasswordResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<EmployeePayrollResponse[]> newPasswordResponse = restTemplate
                .withBasicAuth("diane.evans@acme.com", "newSecurePass123")
                .getForEntity(
                        "/api/empl/payment/",
                        EmployeePayrollResponse[].class
                );
        assertThat(newPasswordResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        EmployeePayrollResponse[] payrolls = newPasswordResponse.getBody();
        assertThat(payrolls).isNotNull();
        assertThat(payrolls).isEmpty();
    }

    @Test
    void changePassword_withShortPassword_returnsBadRequest() {
        SignupRequest request = new SignupRequest(
                "Ethan",
                "Frost",
                "ethan.frost@acme.com",
                "validPassword123"
        );

        ResponseEntity<SignupResponse> signupResponse = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                SignupResponse.class
        );

        assertThat(signupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> changeResponse = restTemplate
                .withBasicAuth("ethan.frost@acme.com", "validPassword123")
                .postForEntity(
                        "/api/auth/changepass",
                        new PasswordChangeRequest("short"),
                        String.class
                );

        assertThat(changeResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> body = parseBody(changeResponse);
        assertThat(body.get("status")).isEqualTo(400);
        assertThat(body.get("error")).isEqualTo("Bad Request");
        assertThat(body.get("message")).isEqualTo("Password length must be 12 chars minimum!");
        assertThat(body.get("path")).isEqualTo("/api/auth/changepass");
    }

    @Test
    void changePassword_withBreachedPassword_returnsBadRequest() {
        SignupRequest request = new SignupRequest(
                "Grace",
                "Hall",
                "grace.hall@acme.com",
                "validPassword456"
        );

        ResponseEntity<SignupResponse> signupResponse = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                SignupResponse.class
        );

        assertThat(signupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> changeResponse = restTemplate
                .withBasicAuth("grace.hall@acme.com", "validPassword456")
                .postForEntity(
                        "/api/auth/changepass",
                        new PasswordChangeRequest("PasswordForFebruary"),
                        String.class
                );

        assertThat(changeResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> body = parseBody(changeResponse);
        assertThat(body.get("status")).isEqualTo(400);
        assertThat(body.get("error")).isEqualTo("Bad Request");
        assertThat(body.get("message")).isEqualTo("The password is in the hacker's database!");
        assertThat(body.get("path")).isEqualTo("/api/auth/changepass");
    }

    @Test
    void changePassword_withSamePassword_returnsBadRequest() {
        SignupRequest request = new SignupRequest(
                "Holly",
                "Irwin",
                "holly.irwin@acme.com",
                "duplicatePass123"
        );

        ResponseEntity<SignupResponse> signupResponse = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                SignupResponse.class
        );

        assertThat(signupResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> changeResponse = restTemplate
                .withBasicAuth("holly.irwin@acme.com", "duplicatePass123")
                .postForEntity(
                        "/api/auth/changepass",
                        new PasswordChangeRequest("duplicatePass123"),
                        String.class
                );

        assertThat(changeResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> body = parseBody(changeResponse);
        assertThat(body.get("status")).isEqualTo(400);
        assertThat(body.get("error")).isEqualTo("Bad Request");
        assertThat(body.get("message")).isEqualTo("The passwords must be different!");
        assertThat(body.get("path")).isEqualTo("/api/auth/changepass");
    }

    @Test
    void payment_withoutAuthentication_returnsUnauthorized() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/empl/payment/",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Map<?, ?> body = objectMapper.readValue(response.getBody(), Map.class);
        assertThat(body.get("status")).isEqualTo(401);
        assertThat(body.get("error")).isEqualTo("Unauthorized");
        assertThat(body.get("message")).isEqualTo("");
        assertThat(body.get("path")).isEqualTo("/api/empl/payment/");
    }
}
