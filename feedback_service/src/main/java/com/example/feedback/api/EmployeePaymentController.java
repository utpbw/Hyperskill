package com.example.feedback.api;

import com.example.feedback.auth.AccountUser;
import com.example.feedback.auth.AccountUserService;
import com.example.feedback.auth.SignupResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/empl/payment", produces = MediaType.APPLICATION_JSON_VALUE)
public class EmployeePaymentController {

    private final AccountUserService accountUserService;

    public EmployeePaymentController(AccountUserService accountUserService) {
        this.accountUserService = accountUserService;
    }

    @GetMapping({"", "/"})
    public SignupResponse payment(Authentication authentication) {
        AccountUser user = accountUserService.findByEmail(authentication.getName());
        return new SignupResponse(user.getId(), user.getName(), user.getLastname(), user.getEmail());
    }
}
