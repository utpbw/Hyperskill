package com.example.feedback.auth;

import com.example.feedback.accounting.EmployeePayrollResponse;
import com.example.feedback.accounting.PayrollRecordRepository;
import com.example.feedback.accounting.PayrollRequest;
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

/**
 * Comprehensive integration tests covering authentication, authorization, and auditing workflows.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountUserRepository accountUserRepository;

    @Autowired
    private PayrollRecordRepository payrollRecordRepository;

    private static final String ADMIN_EMAIL = "admin@acme.com";
    private static final String ADMIN_PASSWORD = "Adm1nPassword123";

    @BeforeEach
    void cleanDatabase() {
        payrollRecordRepository.deleteAll();
        accountUserRepository.deleteAll();
    }

    private Map<?, ?> parseBody(ResponseEntity<String> response) {
        try {
            return objectMapper.readValue(response.getBody(), Map.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse response body", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseList(ResponseEntity<String> response) {
        try {
            return objectMapper.readValue(response.getBody(), List.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse response body", ex);
        }
    }

    private String valueOf(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }

    private void registerDefaultAdmin() {
        registerUser(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private UserResponse registerUser(String email, String password) {
        SignupRequest request = new SignupRequest(
                "John",
                "Doe",
                email,
                password
        );

        ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                UserResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponse body = response.getBody();
        assertThat(body).isNotNull();
        return body;
    }

    private <T> ResponseEntity<T> exchangeRole(String adminEmail, String adminPassword, RoleUpdateRequest request, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return restTemplate
                .withBasicAuth(adminEmail, adminPassword)
                .exchange(
                        "/api/admin/user/role",
                        HttpMethod.PUT,
                        new HttpEntity<>(request, headers),
                        responseType
                );
    }

    private UserResponse grantRole(String adminEmail, String adminPassword, String userEmail, String role) {
        RoleUpdateRequest request = new RoleUpdateRequest(userEmail, role, RoleOperation.GRANT);
        ResponseEntity<UserResponse> response = exchangeRole(adminEmail, adminPassword, request, UserResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private ResponseEntity<UserDeletionResponse> deleteUser(String adminEmail, String adminPassword, String email) {
        return restTemplate
                .withBasicAuth(adminEmail, adminPassword)
                .exchange(
                        "/api/admin/user/" + email,
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        UserDeletionResponse.class
                );
    }

    @Test
    void signup_withValidRequest_returnsCreatedUserDetails() {
        SignupRequest request = new SignupRequest(
                "John",
                "Doe",
                "john.doe@acme.com",
                "password12345"
        );

        ResponseEntity<UserResponse> response = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                UserResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.name()).isEqualTo("John");
        assertThat(body.lastname()).isEqualTo("Doe");
        assertThat(body.email()).isEqualTo("john.doe@acme.com");
        assertThat(body.roles()).containsExactly("ROLE_ADMINISTRATOR");
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

        ResponseEntity<UserResponse> firstResponse = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                UserResponse.class
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
        registerDefaultAdmin();
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
        registerDefaultAdmin();
        registerUser("jane.payroll@acme.com", "verystrongpass");
        grantRole(ADMIN_EMAIL, ADMIN_PASSWORD, "jane.payroll@acme.com", "ACCOUNTANT");

        PayrollRequest payroll = new PayrollRequest("jane.payroll@acme.com", "01-2024", 1_000_00L);
        ResponseEntity<Map> response = restTemplate
                .withBasicAuth("jane.payroll@acme.com", "verystrongpass")
                .postForEntity(
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
        registerDefaultAdmin();
        registerUser("duplicate@acme.com", "anotherstrongpass");
        grantRole(ADMIN_EMAIL, ADMIN_PASSWORD, "duplicate@acme.com", "ACCOUNTANT");

        PayrollRequest payroll = new PayrollRequest("duplicate@acme.com", "02-2024", 120_000L);
        ResponseEntity<Map> firstResponse = restTemplate
                .withBasicAuth("duplicate@acme.com", "anotherstrongpass")
                .postForEntity(
                        "/api/acct/payments",
                        List.of(payroll),
                        Map.class
                );

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> secondResponse = restTemplate
                .withBasicAuth("duplicate@acme.com", "anotherstrongpass")
                .postForEntity(
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
        registerDefaultAdmin();
        registerUser("accountant@acme.com", "accPassStrong");
        grantRole(ADMIN_EMAIL, ADMIN_PASSWORD, "accountant@acme.com", "ACCOUNTANT");

        PayrollRequest payroll = new PayrollRequest("unknown@acme.com", "03-2024", 500_00L);

        ResponseEntity<String> response = restTemplate
                .withBasicAuth("accountant@acme.com", "accPassStrong")
                .postForEntity(
                        "/api/acct/payments",
                        List.of(payroll),
                        String.class
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> body = parseBody(response);
        assertThat(body.get("message")).isNotNull();
    }

    @Test
    void uploadPayments_withMultipleValidationErrors_returnsAggregatedMessage() {
        registerDefaultAdmin();
        registerUser("aggregate@acme.com", "averysecurepwd");
        grantRole(ADMIN_EMAIL, ADMIN_PASSWORD, "aggregate@acme.com", "ACCOUNTANT");

        List<PayrollRequest> payload = List.of(
                new PayrollRequest("aggregate@acme.com", "01-2024", -1L),
                new PayrollRequest("aggregate@acme.com", "13-2024", 1_000_00L)
        );

        ResponseEntity<String> response = restTemplate
                .withBasicAuth("aggregate@acme.com", "averysecurepwd")
                .postForEntity(
                        "/api/acct/payments",
                        payload,
                        String.class
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> body = parseBody(response);
        assertThat(body.get("message")).isEqualTo("payments[0].salary: Salary must be non negative!, payments[1].period: Wrong date!");
        assertThat(body.get("path")).isEqualTo("/api/acct/payments");
    }

    @Test
    void updatePayments_withValidRequest_updatesSalary() {
        registerDefaultAdmin();
        registerUser("update@acme.com", "superstrongpass");
        grantRole(ADMIN_EMAIL, ADMIN_PASSWORD, "update@acme.com", "ACCOUNTANT");
        PayrollRequest initial = new PayrollRequest("update@acme.com", "04-2024", 100_00L);

        ResponseEntity<Map> uploadResponse = restTemplate
                .withBasicAuth("update@acme.com", "superstrongpass")
                .postForEntity(
                        "/api/acct/payments",
                        List.of(initial),
                        Map.class
                );
        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        PayrollRequest update = new PayrollRequest("update@acme.com", "04-2024", 150_00L);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> updateResponse = restTemplate
                .withBasicAuth("update@acme.com", "superstrongpass")
                .exchange(
                        "/api/acct/payments",
                        HttpMethod.PUT,
                        new HttpEntity<>(update, headers),
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
        registerDefaultAdmin();
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
        registerDefaultAdmin();
        SignupRequest request = new SignupRequest(
                "Bob",
                "Stone",
                "bob.stone@acme.com",
                "topsecretpass"
        );

        ResponseEntity<UserResponse> signupResponse = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                UserResponse.class
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
        registerDefaultAdmin();
        SignupRequest request = new SignupRequest(
                "Carol",
                "Mills",
                "carol.mills@acme.com",
                "pass1234secure"
        );

        ResponseEntity<UserResponse> signupResponse = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                UserResponse.class
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
        registerDefaultAdmin();
        SignupRequest request = new SignupRequest(
                "Diane",
                "Evans",
                "diane.evans@acme.com",
                "initialPass123"
        );

        ResponseEntity<UserResponse> signupResponse = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                UserResponse.class
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
        registerDefaultAdmin();
        SignupRequest request = new SignupRequest(
                "Ethan",
                "Frost",
                "ethan.frost@acme.com",
                "validPassword123"
        );

        ResponseEntity<UserResponse> signupResponse = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                UserResponse.class
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
        registerDefaultAdmin();
        SignupRequest request = new SignupRequest(
                "Grace",
                "Hall",
                "grace.hall@acme.com",
                "validPassword456"
        );

        ResponseEntity<UserResponse> signupResponse = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                UserResponse.class
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
        registerDefaultAdmin();
        SignupRequest request = new SignupRequest(
                "Holly",
                "Irwin",
                "holly.irwin@acme.com",
                "duplicatePass123"
        );

        ResponseEntity<UserResponse> signupResponse = restTemplate.postForEntity(
                "/api/auth/signup",
                request,
                UserResponse.class
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
    void admin_getUsers_returnsSortedUsersWithRoles() {
        registerDefaultAdmin();
        registerUser("user.one@acme.com", "StrongPass1234");
        registerUser("user.two@acme.com", "StrongPass5678");

        ResponseEntity<UserResponse[]> response = restTemplate
                .withBasicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)
                .getForEntity("/api/admin/user", UserResponse[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserResponse[] users = response.getBody();
        assertThat(users).isNotNull();
        assertThat(users).hasSize(3);
        assertThat(users[0].email()).isEqualTo(ADMIN_EMAIL);
        assertThat(users[0].roles()).containsExactly("ROLE_ADMINISTRATOR");
        assertThat(users[1].email()).isEqualTo("user.one@acme.com");
        assertThat(users[1].roles()).containsExactly("ROLE_USER");
        assertThat(users[2].email()).isEqualTo("user.two@acme.com");
        assertThat(users[2].roles()).containsExactly("ROLE_USER");
    }

    @Test
    void admin_deleteUser_removesUserSuccessfully() {
        registerDefaultAdmin();
        registerUser("delete.me@acme.com", "DeletePass123");

        ResponseEntity<UserDeletionResponse> deleteResponse = deleteUser(ADMIN_EMAIL, ADMIN_PASSWORD, "delete.me@acme.com");
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserDeletionResponse body = deleteResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(body.user()).isEqualTo("delete.me@acme.com");
        assertThat(body.status()).isEqualTo("Deleted successfully!");

        ResponseEntity<UserResponse[]> remaining = restTemplate
                .withBasicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)
                .getForEntity("/api/admin/user", UserResponse[].class);
        assertThat(remaining.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(remaining.getBody()).isNotNull().hasSize(1);
        assertThat(remaining.getBody()[0].email()).isEqualTo(ADMIN_EMAIL);
    }

    @Test
    void admin_deleteUser_nonExistingUserReturnsNotFound() {
        registerDefaultAdmin();

        ResponseEntity<String> response = restTemplate
                .withBasicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)
                .exchange(
                        "/api/admin/user/missing@acme.com",
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        String.class
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Map<?, ?> body = parseBody(response);
        assertThat(body.get("message")).isEqualTo("User not found!");
        assertThat(body.get("path")).isEqualTo("/api/admin/user/missing@acme.com");
    }

    @Test
    void admin_deleteUser_cannotRemoveAdministrator() {
        registerDefaultAdmin();

        ResponseEntity<String> response = restTemplate
                .withBasicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)
                .exchange(
                        "/api/admin/user/" + ADMIN_EMAIL,
                        HttpMethod.DELETE,
                        HttpEntity.EMPTY,
                        String.class
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> body = parseBody(response);
        assertThat(body.get("message")).isEqualTo("Can't remove ADMINISTRATOR role!");
        assertThat(body.get("path")).isEqualTo("/api/admin/user/" + ADMIN_EMAIL);
    }

    @Test
    void admin_updateUserRole_grantAndRemoveRoles() {
        registerDefaultAdmin();
        registerUser("accountant.role@acme.com", "AccountantPass123");

        UserResponse afterGrant = grantRole(ADMIN_EMAIL, ADMIN_PASSWORD, "accountant.role@acme.com", "ACCOUNTANT");
        assertThat(afterGrant.roles()).containsExactly("ROLE_ACCOUNTANT", "ROLE_USER");

        RoleUpdateRequest removeRequest = new RoleUpdateRequest("accountant.role@acme.com", "ACCOUNTANT", RoleOperation.REMOVE);
        ResponseEntity<UserResponse> removeResponse = exchangeRole(ADMIN_EMAIL, ADMIN_PASSWORD, removeRequest, UserResponse.class);
        assertThat(removeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(removeResponse.getBody()).isNotNull();
        assertThat(removeResponse.getBody().roles()).containsExactly("ROLE_USER");
    }

    @Test
    void admin_updateUserRole_preventsMixingAdministrativeAndBusinessRoles() {
        registerDefaultAdmin();
        registerUser("business.role@acme.com", "BusinessPass123");

        RoleUpdateRequest request = new RoleUpdateRequest("business.role@acme.com", "ADMINISTRATOR", RoleOperation.GRANT);
        ResponseEntity<String> response = exchangeRole(ADMIN_EMAIL, ADMIN_PASSWORD, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> body = parseBody(response);
        assertThat(body.get("message")).isEqualTo("The user cannot combine administrative and business roles!");
        assertThat(body.get("path")).isEqualTo("/api/admin/user/role");
    }

    @Test
    void admin_updateUserRole_roleNotFoundReturnsNotFound() {
        registerDefaultAdmin();
        registerUser("unknown.role@acme.com", "UnknownPass123");

        RoleUpdateRequest request = new RoleUpdateRequest("unknown.role@acme.com", "MANAGER", RoleOperation.GRANT);
        ResponseEntity<String> response = exchangeRole(ADMIN_EMAIL, ADMIN_PASSWORD, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        Map<?, ?> body = parseBody(response);
        assertThat(body.get("message")).isEqualTo("Role not found!");
        assertThat(body.get("path")).isEqualTo("/api/admin/user/role");
    }

    @Test
    void admin_updateUserRole_removeNonExistingRoleReturnsBadRequest() {
        registerDefaultAdmin();
        registerUser("no.role@acme.com", "NoRolePass123");

        RoleUpdateRequest request = new RoleUpdateRequest("no.role@acme.com", "ACCOUNTANT", RoleOperation.REMOVE);
        ResponseEntity<String> response = exchangeRole(ADMIN_EMAIL, ADMIN_PASSWORD, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> body = parseBody(response);
        assertThat(body.get("message")).isEqualTo("The user does not have a role!");
        assertThat(body.get("path")).isEqualTo("/api/admin/user/role");
    }

    @Test
    void admin_updateUserRole_removeLastRoleReturnsBadRequest() {
        registerDefaultAdmin();
        registerUser("single.role@acme.com", "SingleRolePass123");

        RoleUpdateRequest request = new RoleUpdateRequest("single.role@acme.com", "USER", RoleOperation.REMOVE);
        ResponseEntity<String> response = exchangeRole(ADMIN_EMAIL, ADMIN_PASSWORD, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> body = parseBody(response);
        assertThat(body.get("message")).isEqualTo("The user must have at least one role!");
        assertThat(body.get("path")).isEqualTo("/api/admin/user/role");
    }

    @Test
    void admin_accessDeniedForNonAdminUser_returnsForbiddenErrorBody() {
        registerDefaultAdmin();
        registerUser("regular.user@acme.com", "RegularPass123");

        ResponseEntity<String> response = restTemplate
                .withBasicAuth("regular.user@acme.com", "RegularPass123")
                .getForEntity("/api/admin/user", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Map<?, ?> body = parseBody(response);
        assertThat(body.get("status")).isEqualTo(403);
        assertThat(body.get("error")).isEqualTo("Forbidden");
        assertThat(body.get("message")).isEqualTo("Access Denied!");
        assertThat(body.get("path")).isEqualTo("/api/admin/user");
    }

    @Test
    void securityEvents_captureKeyActions() {
        registerUser(ADMIN_EMAIL, ADMIN_PASSWORD);
        registerUser("petrpetrov@acme.com", "UserPassword123");
        registerUser("maxmustermann@acme.com", "MaxSecurePass123");
        registerUser("auditor@acme.com", "AuditorPass123");

        grantRole(ADMIN_EMAIL, ADMIN_PASSWORD, "auditor@acme.com", "AUDITOR");
        grantRole(ADMIN_EMAIL, ADMIN_PASSWORD, "petrpetrov@acme.com", "ACCOUNTANT");

        RoleUpdateRequest removeAccountant = new RoleUpdateRequest("petrpetrov@acme.com", "ACCOUNTANT", RoleOperation.REMOVE);
        ResponseEntity<UserResponse> removeResponse = exchangeRole(ADMIN_EMAIL, ADMIN_PASSWORD, removeAccountant, UserResponse.class);
        assertThat(removeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<UserDeletionResponse> deleteResponse = deleteUser(ADMIN_EMAIL, ADMIN_PASSWORD, "petrpetrov@acme.com");
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> accessDeniedResponse = restTemplate
                .withBasicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)
                .postForEntity(
                        "/api/acct/payments",
                        new HttpEntity<>("[]", jsonHeaders),
                        String.class
                );
        assertThat(accessDeniedResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> failureResponse = restTemplate
                    .withBasicAuth("maxmustermann@acme.com", "WrongPassword123")
                    .getForEntity("/api/empl/payment", String.class);
            assertThat(failureResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        HttpHeaders accessHeaders = new HttpHeaders();
        accessHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<UserAccessStatusResponse> unlockResponse = restTemplate
                .withBasicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)
                .exchange(
                        "/api/admin/user/access",
                        HttpMethod.PUT,
                        new HttpEntity<>(new UserAccessRequest("maxmustermann@acme.com", UserAccessOperation.UNLOCK), accessHeaders),
                        UserAccessStatusResponse.class
                );
        assertThat(unlockResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unlockResponse.getBody()).isNotNull();
        assertThat(unlockResponse.getBody().status()).isEqualTo("User maxmustermann@acme.com unlocked!");

        ResponseEntity<PasswordChangeResponse> passwordChangeResponse = restTemplate
                .withBasicAuth(ADMIN_EMAIL, ADMIN_PASSWORD)
                .postForEntity(
                        "/api/auth/changepass",
                        new PasswordChangeRequest("NewAdm1nPassword456"),
                        PasswordChangeResponse.class
                );
        assertThat(passwordChangeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> eventsResponse = restTemplate
                .withBasicAuth("auditor@acme.com", "AuditorPass123")
                .getForEntity("/api/security/events", String.class);
        assertThat(eventsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<Map<String, Object>> events = parseList(eventsResponse);
        assertThat(events).isNotEmpty();
        List<Long> ids = events.stream()
                .map(event -> ((Number) event.get("id")).longValue())
                .toList();
        assertThat(ids).isSorted();

        assertThat(events).anyMatch(event ->
                "CREATE_USER".equals(valueOf(event, "action")) &&
                        "Anonymous".equals(valueOf(event, "subject")) &&
                        ADMIN_EMAIL.equals(valueOf(event, "object")) &&
                        "/api/auth/signup".equals(valueOf(event, "path")));

        assertThat(events).anyMatch(event ->
                "GRANT_ROLE".equals(valueOf(event, "action")) &&
                        ADMIN_EMAIL.equals(valueOf(event, "subject")) &&
                        "Grant role ACCOUNTANT to petrpetrov@acme.com".equals(valueOf(event, "object")));

        assertThat(events).anyMatch(event ->
                "REMOVE_ROLE".equals(valueOf(event, "action")) &&
                        ADMIN_EMAIL.equals(valueOf(event, "subject")) &&
                        "Remove role ACCOUNTANT from petrpetrov@acme.com".equals(valueOf(event, "object")));

        assertThat(events).anyMatch(event ->
                "DELETE_USER".equals(valueOf(event, "action")) &&
                        ADMIN_EMAIL.equals(valueOf(event, "subject")) &&
                        "petrpetrov@acme.com".equals(valueOf(event, "object")) &&
                        "/api/admin/user".equals(valueOf(event, "path")));

        assertThat(events).anyMatch(event ->
                "ACCESS_DENIED".equals(valueOf(event, "action")) &&
                        ADMIN_EMAIL.equals(valueOf(event, "subject")) &&
                        "/api/acct/payments".equals(valueOf(event, "object")) &&
                        "/api/acct/payments".equals(valueOf(event, "path")));

        assertThat(events).anyMatch(event ->
                "LOGIN_FAILED".equals(valueOf(event, "action")) &&
                        "maxmustermann@acme.com".equals(valueOf(event, "subject")) &&
                        "/api/empl/payment".equals(valueOf(event, "object")));

        assertThat(events).anyMatch(event ->
                "BRUTE_FORCE".equals(valueOf(event, "action")) &&
                        "maxmustermann@acme.com".equals(valueOf(event, "subject")) &&
                        "/api/empl/payment".equals(valueOf(event, "object")));

        assertThat(events).anyMatch(event ->
                "LOCK_USER".equals(valueOf(event, "action")) &&
                        "maxmustermann@acme.com".equals(valueOf(event, "subject")) &&
                        "Lock user maxmustermann@acme.com".equals(valueOf(event, "object")) &&
                        "/api/empl/payment".equals(valueOf(event, "path")));

        assertThat(events).anyMatch(event ->
                "UNLOCK_USER".equals(valueOf(event, "action")) &&
                        ADMIN_EMAIL.equals(valueOf(event, "subject")) &&
                        "Unlock user maxmustermann@acme.com".equals(valueOf(event, "object")) &&
                        "/api/admin/user/access".equals(valueOf(event, "path")));

        assertThat(events).anyMatch(event ->
                "CHANGE_PASSWORD".equals(valueOf(event, "action")) &&
                        ADMIN_EMAIL.equals(valueOf(event, "subject")) &&
                        ADMIN_EMAIL.equals(valueOf(event, "object")) &&
                        "/api/auth/changepass".equals(valueOf(event, "path")));
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
