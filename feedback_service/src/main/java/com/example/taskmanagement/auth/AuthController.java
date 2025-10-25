package com.example.taskmanagement.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final AccountUserService accountUserService;
    private final TokenService tokenService;

    public AuthController(AccountUserService accountUserService, TokenService tokenService) {
        this.accountUserService = accountUserService;
        this.tokenService = tokenService;
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

    @PostMapping(path = "/token")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponse token(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        AccountUser user = accountUserService.findByEmail(authentication.getName());
        String token = tokenService.issueToken(user);
        return new TokenResponse(token);
    }
}
