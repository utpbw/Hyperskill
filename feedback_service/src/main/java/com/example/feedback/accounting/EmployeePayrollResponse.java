package com.example.feedback.accounting;

public record EmployeePayrollResponse(
        String name,
        String lastname,
        String period,
        String salary
) {
}
