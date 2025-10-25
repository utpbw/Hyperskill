package com.example.taskmanagement.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(
        @JsonProperty("new_password")
        @NotBlank(message = "New password must not be blank")
        String newPassword
) {
}
