package com.hyperskill.tracker;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeveloperRequest(
    @NotNull @Email String email,
    @NotBlank String password
) {
}
