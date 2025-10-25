package com.example.feedback.accounting;

/**
 * Response payload describing payroll information for a single employee period.
 */
public record EmployeePayrollResponse(
        String name,
        String lastname,
        String period,
        String salary
) {
}
