package com.hyperskill.tracker;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApplicationRegistrationRequest(
    @NotNull @NotBlank String name,
    @NotNull String description
) {
}
