package com.example.feedback.accounting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record PayrollRequest(
        @NotBlank(message = "Employee email must not be blank")
        String employee,

        @NotBlank(message = "Period must not be blank")
        @ValidPeriod
        String period,

        @NotNull(message = "Salary must be provided")
        @PositiveOrZero(message = "Salary must be non negative!")
        Long salary
) {
}
