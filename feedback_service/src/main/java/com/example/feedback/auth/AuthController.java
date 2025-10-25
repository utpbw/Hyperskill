package com.example.feedback.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing endpoints for user registration and password management.
 */
@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final AccountUserService accountUserService;

    public AuthController(AccountUserService accountUserService) {
        this.accountUserService = accountUserService;
    }

    @PostMapping(path = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserResponse signup(HttpServletRequest httpRequest, @Valid @RequestBody SignupRequest request) {
        return accountUserService.registerUser(request, httpRequest.getRequestURI());
    }

    @PostMapping(path = "/changepass", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PasswordChangeResponse changePassword(
            Authentication authentication,
            HttpServletRequest httpRequest,
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        return accountUserService.changePassword(authentication.getName(), request.newPassword(),
                httpRequest.getRequestURI());
    }
}
