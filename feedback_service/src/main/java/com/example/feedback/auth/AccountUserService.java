package com.example.feedback.auth;

import com.example.feedback.security.SecurityEventAction;
import com.example.feedback.security.SecurityEventService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AccountUserService {

    private final AccountUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityEventService securityEventService;

    private static final Set<String> BREACHED_PASSWORDS = Set.of(
            "PasswordForJanuary",
            "PasswordForFebruary",
            "PasswordForMarch",
            "PasswordForApril",
            "PasswordForMay",
            "PasswordForJune",
            "PasswordForJuly",
            "PasswordForAugust",
            "PasswordForSeptember",
            "PasswordForOctober",
            "PasswordForNovember",
            "PasswordForDecember"
    );

    public AccountUserService(AccountUserRepository repository,
                              PasswordEncoder passwordEncoder,
                              SecurityEventService securityEventService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.securityEventService = securityEventService;
    }

    @Transactional
    public UserResponse registerUser(SignupRequest request, String path) {
        String normalizedEmail = normalizeEmail(request.email());
        String sanitizedName = request.name().trim();
        String sanitizedLastname = request.lastname().trim();
        validatePassword(request.password());
        if (repository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User exist!");
        }

        AccountUser user = new AccountUser();
        user.setName(sanitizedName);
        user.setLastname(sanitizedLastname);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        Set<UserRole> roles = assignInitialRoles();
        user.setRoles(roles);

        AccountUser saved = repository.save(user);
        securityEventService.logEvent(SecurityEventAction.CREATE_USER,
                SecurityEventService.ANONYMOUS_SUBJECT,
                saved.getEmail(),
                path);
        roles.forEach(role -> securityEventService.logEvent(SecurityEventAction.GRANT_ROLE,
                SecurityEventService.ANONYMOUS_SUBJECT,
                "Grant role " + role.name() + " to " + saved.getEmail(),
                path));
        return toResponse(saved);
    }

    @Transactional
    public PasswordChangeResponse changePassword(String email, String newPassword, String path) {
        AccountUser user = findByEmail(email);
        validatePassword(newPassword);
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The passwords must be different!");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedAttempts(0);
        repository.save(user);
        securityEventService.logEvent(SecurityEventAction.CHANGE_PASSWORD,
                user.getEmail(),
                user.getEmail(),
                path);
        return new PasswordChangeResponse(user.getEmail(), "The password has been updated successfully");
    }

    @Transactional(readOnly = true)
    public AccountUser findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        return repository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAllUsers() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDeletionResponse deleteUser(String subjectEmail, String email, String path) {
        String normalizedEmail = normalizeEmail(email);
        AccountUser user = repository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!"));

        if (user.getRoles().contains(UserRole.ROLE_ADMINISTRATOR)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't remove ADMINISTRATOR role!");
        }

        repository.delete(user);
        securityEventService.logEvent(SecurityEventAction.DELETE_USER,
                normalizeEmail(subjectEmail),
                "Delete user " + user.getEmail(),
                path);
        return new UserDeletionResponse(user.getEmail(), "Deleted successfully!");
    }

    @Transactional
    public UserResponse updateUserRole(String subjectEmail, RoleUpdateRequest request, String path) {
        String normalizedEmail = normalizeEmail(request.user());
        AccountUser user = repository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!"));

        UserRole role = UserRole.fromName(request.role())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found!"));

        String subject = normalizeEmail(subjectEmail);
        if (request.operation() == RoleOperation.GRANT) {
            boolean added = grantRole(user, role);
            AccountUser saved = repository.save(user);
            if (added) {
                securityEventService.logEvent(SecurityEventAction.GRANT_ROLE,
                        subject,
                        "Grant role " + role.name() + " to " + saved.getEmail(),
                        path);
            }
            return toResponse(saved);
        } else {
            removeRole(user, role);
            AccountUser saved = repository.save(user);
            securityEventService.logEvent(SecurityEventAction.REMOVE_ROLE,
                    subject,
                    "Remove role " + role.name() + " from " + saved.getEmail(),
                    path);
            return toResponse(saved);
        }
    }

    @Transactional
    public UserAccessStatusResponse updateUserAccess(String subjectEmail, UserAccessRequest request, String path) {
        String normalizedEmail = normalizeEmail(request.user());
        AccountUser user = repository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found!"));

        if (request.operation() == UserAccessOperation.LOCK) {
            if (user.getRoles().contains(UserRole.ROLE_ADMINISTRATOR)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't lock the ADMINISTRATOR!");
            }

            user.setLocked(true);
            user.setFailedAttempts(0);
            repository.save(user);
            securityEventService.logEvent(SecurityEventAction.LOCK_USER,
                    normalizeEmail(subjectEmail),
                    "Lock user " + user.getEmail(),
                    path);
            return new UserAccessStatusResponse("User " + user.getEmail() + " locked!");
        } else {
            user.setLocked(false);
            user.setFailedAttempts(0);
            repository.save(user);
            securityEventService.logEvent(SecurityEventAction.UNLOCK_USER,
                    normalizeEmail(subjectEmail),
                    "Unlock user " + user.getEmail(),
                    path);
            return new UserAccessStatusResponse("User " + user.getEmail() + " unlocked!");
        }
    }

    private void validatePassword(String password) {
        if (password.length() < 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password length must be 12 chars minimum!");
        }

        if (BREACHED_PASSWORDS.contains(password)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The password is in the hacker's database!");
        }
    }

    private Set<UserRole> assignInitialRoles() {
        Set<UserRole> roles = new HashSet<>();
        if (repository.count() == 0) {
            roles.add(UserRole.ROLE_ADMINISTRATOR);
        } else {
            roles.add(UserRole.ROLE_USER);
        }
        return roles;
    }

    private boolean grantRole(AccountUser user, UserRole role) {
        boolean hasAdministrative = user.getRoles().stream().anyMatch(UserRole::isAdministrative);
        boolean hasBusiness = user.getRoles().stream().anyMatch(UserRole::isBusiness);

        if (role.isAdministrative() && hasBusiness) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The user cannot combine administrative and business roles!");
        }

        if (role.isBusiness() && hasAdministrative) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The user cannot combine administrative and business roles!");
        }

        if (user.getRoles().contains(role)) {
            return false;
        }

        user.getRoles().add(role);
        return true;
    }

    private void removeRole(AccountUser user, UserRole role) {
        if (!user.getRoles().contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The user does not have a role!");
        }

        if (role == UserRole.ROLE_ADMINISTRATOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can't remove ADMINISTRATOR role!");
        }

        if (user.getRoles().size() == 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The user must have at least one role!");
        }

        user.getRoles().remove(role);
    }

    private UserResponse toResponse(AccountUser user) {
        List<String> roles = user.getRoles().stream()
                .map(UserRole::name)
                .sorted()
                .collect(Collectors.toList());
        return new UserResponse(user.getId(), user.getName(), user.getLastname(), user.getEmail(), roles);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
