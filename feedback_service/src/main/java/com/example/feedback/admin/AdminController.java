package com.example.feedback.admin;

import com.example.feedback.auth.AccountUserService;
import com.example.feedback.auth.RoleUpdateRequest;
import com.example.feedback.auth.UserDeletionResponse;
import com.example.feedback.auth.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public UserDeletionResponse deleteUser(@PathVariable("email") String email) {
        return accountUserService.deleteUser(email);
    }

    @PutMapping(path = "/role", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserResponse updateUserRole(@Valid @RequestBody RoleUpdateRequest request) {
        return accountUserService.updateUserRole(request);
    }
}
