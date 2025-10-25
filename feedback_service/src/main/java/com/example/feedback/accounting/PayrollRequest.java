package com.example.feedback.accounting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PayrollRequest(
        @NotBlank(message = "Employee email must not be blank")
        String employee,

        @NotBlank(message = "Period must not be blank")
        String period,

        @NotNull(message = "Salary must be provided")
        Long salary
) {
}
