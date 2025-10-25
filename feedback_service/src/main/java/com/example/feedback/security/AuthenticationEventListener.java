package com.example.feedback.security;

import com.example.feedback.auth.AccountUser;
import com.example.feedback.auth.AccountUserRepository;
import com.example.feedback.auth.UserRole;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Optional;

/**
 * Spring application event listener that tracks login successes and failures for auditing and lockout.
 */
@Component
public class AuthenticationEventListener {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final SecurityEventService securityEventService;
    private final AccountUserRepository accountUserRepository;

    public AuthenticationEventListener(SecurityEventService securityEventService,
                                       AccountUserRepository accountUserRepository) {
        this.securityEventService = securityEventService;
        this.accountUserRepository = accountUserRepository;
    }

    @EventListener
    @Transactional
    public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        Authentication authentication = event.getAuthentication();
        if (authentication == null) {
            return;
        }

        String username = authentication.getName();
        String normalizedUsername = username == null ? SecurityEventService.ANONYMOUS_SUBJECT
                : username.trim().toLowerCase(Locale.ROOT);
        String path = resolvePath();

        securityEventService.logEvent(SecurityEventAction.LOGIN_FAILED,
                normalizedUsername,
                path,
                path);

        if (event instanceof AuthenticationFailureBadCredentialsEvent) {
            Optional<AccountUser> optionalUser = accountUserRepository.findByEmailIgnoreCase(normalizedUsername);
            if (optionalUser.isPresent()) {
                AccountUser user = optionalUser.get();
                int attempts = user.getFailedAttempts() + 1;
                user.setFailedAttempts(attempts);
                if (!user.isLocked() && attempts >= MAX_FAILED_ATTEMPTS) {
                    securityEventService.logEvent(SecurityEventAction.BRUTE_FORCE,
                            user.getEmail(),
                            path,
                            path);
                    if (!user.getRoles().contains(UserRole.ROLE_ADMINISTRATOR)) {
                        user.setLocked(true);
                        securityEventService.logEvent(SecurityEventAction.LOCK_USER,
                                user.getEmail(),
                                "Lock user " + user.getEmail(),
                                path);
                    }
                }
                accountUserRepository.save(user);
            }
        }
    }

    @EventListener
    @Transactional
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        if (authentication == null) {
            return;
        }

        String username = authentication.getName();
        if (username == null) {
            return;
        }

        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        accountUserRepository.findByEmailIgnoreCase(normalizedUsername)
                .ifPresent(user -> {
                    user.setFailedAttempts(0);
                    accountUserRepository.save(user);
                });
    }

    private String resolvePath() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            if (request != null) {
                return request.getRequestURI();
            }
        }
        return "";
    }
}
