package com.example.taskmanagement.accounting;

public record EmployeePayrollResponse(
        String name,
        String lastname,
        String period,
        String salary
) {
}
