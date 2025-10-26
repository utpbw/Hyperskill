package com.hyperskill.tracker;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ApplicationRegistrationRequest(
    @NotNull @NotBlank String name,
    @NotNull String description,
    @NotNull @NotBlank @Pattern(regexp = "basic|premium") String category
) {
}
