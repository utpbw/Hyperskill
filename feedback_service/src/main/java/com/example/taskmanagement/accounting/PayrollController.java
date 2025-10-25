package com.example.taskmanagement.accounting;

import com.example.taskmanagement.auth.AccountUserService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class PayrollController {

    private final PayrollService payrollService;
    private final AccountUserService accountUserService;
    private final Validator validator;

    public PayrollController(PayrollService payrollService, AccountUserService accountUserService, Validator validator) {
        this.payrollService = payrollService;
        this.accountUserService = accountUserService;
        this.validator = validator;
    }

    @PostMapping(path = "/acct/payments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> uploadPayments(@RequestBody List<PayrollRequest> payments) {
        validatePayments(payments);
        payrollService.uploadPayrolls(payments);
        return Collections.singletonMap("status", "Added successfully!");
    }

    @PutMapping(path = "/acct/payments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> updatePayment(@RequestBody PayrollRequest payment) {
        validatePayment(payment);
        payrollService.updatePayroll(payment);
        return Collections.singletonMap("status", "Updated successfully!");
    }

    @GetMapping(path = {"/empl/payment", "/empl/payment/"})
    public ResponseEntity<?> getPayments(
            Authentication authentication,
            @RequestParam(name = "period", required = false) String period
    ) {
        var user = accountUserService.findByEmail(authentication.getName());
        if (period == null) {
            List<EmployeePayrollResponse> payrolls = payrollService.getPayrollsFor(user);
            return ResponseEntity.ok(payrolls);
        }

        return payrollService.getPayrollFor(user, period)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(Collections.emptyMap()));
    }

    private void validatePayments(List<PayrollRequest> payments) {
        if (payments == null) {
            throw new ResponseStatusException(BAD_REQUEST, "payments: must not be null");
        }

        List<String> errors = new ArrayList<>();
        for (int index = 0; index < payments.size(); index++) {
            PayrollRequest payment = payments.get(index);
            if (payment == null) {
                errors.add(String.format("payments[%d]: must not be null", index));
                continue;
            }

            for (ConstraintViolation<PayrollRequest> violation : validator.validate(payment)) {
                errors.add(String.format("payments[%d].%s: %s", index, violation.getPropertyPath(), violation.getMessage()));
            }
        }

        if (!errors.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, String.join(", ", errors));
        }
    }

    private void validatePayment(PayrollRequest payment) {
        if (payment == null) {
            throw new ResponseStatusException(BAD_REQUEST, "payment: must not be null");
        }

        List<String> errors = new ArrayList<>();
        for (ConstraintViolation<PayrollRequest> violation : validator.validate(payment)) {
            errors.add(String.format("payment.%s: %s", violation.getPropertyPath(), violation.getMessage()));
        }

        if (!errors.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, String.join(", ", errors));
        }
    }
}
