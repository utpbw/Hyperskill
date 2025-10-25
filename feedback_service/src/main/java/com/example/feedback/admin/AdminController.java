package com.example.feedback.admin;

import com.example.feedback.auth.AccountUserService;
import com.example.feedback.auth.RoleUpdateRequest;
import com.example.feedback.auth.UserAccessRequest;
import com.example.feedback.auth.UserAccessStatusResponse;
import com.example.feedback.auth.UserDeletionResponse;
import com.example.feedback.auth.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Administrative REST controller for managing users, roles, and account access policies.
 */
@RestController
@RequestMapping(path = "/api/admin/user", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminController {

    private final AccountUserService accountUserService;

    public AdminController(AccountUserService accountUserService) {
        this.accountUserService = accountUserService;
    }

    @GetMapping
    public List<UserResponse> findAllUsers() {
        return accountUserService.findAllUsers();
    }

    @DeleteMapping(path = "/{email}")
    public UserDeletionResponse deleteUser(Authentication authentication,
                                          HttpServletRequest request,
                                          @PathVariable("email") String email) {
        return accountUserService.deleteUser(authentication.getName(), email, request.getRequestURI());
    }

    @PutMapping(path = "/role", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserResponse updateUserRole(Authentication authentication,
                                       HttpServletRequest requestContext,
                                       @Valid @RequestBody RoleUpdateRequest request) {
        return accountUserService.updateUserRole(authentication.getName(), request,
                requestContext.getRequestURI());
    }

    @PutMapping(path = "/access", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserAccessStatusResponse updateUserAccess(Authentication authentication,
                                                     HttpServletRequest requestContext,
                                                     @Valid @RequestBody UserAccessRequest request) {
        return accountUserService.updateUserAccess(authentication.getName(), request,
                requestContext.getRequestURI());
    }
}
