package com.example.feedback.accounting;

import com.example.feedback.auth.AccountUserService;
import jakarta.validation.Valid;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class PayrollController {

    private final PayrollService payrollService;
    private final AccountUserService accountUserService;

    public PayrollController(PayrollService payrollService, AccountUserService accountUserService) {
        this.payrollService = payrollService;
        this.accountUserService = accountUserService;
    }

    @PostMapping(path = "/acct/payments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> uploadPayments(@Valid @RequestBody List<@Valid PayrollRequest> requests) {
        payrollService.uploadPayrolls(requests);
        return Collections.singletonMap("status", "Added successfully!");
    }

    @PutMapping(path = "/acct/payments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> updatePayment(@Valid @RequestBody PayrollRequest request) {
        payrollService.updatePayroll(request);
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
}
