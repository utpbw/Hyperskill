package com.example.feedback.config;

import com.example.feedback.auth.AccountUser;
import com.example.feedback.auth.AccountUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Locale;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    public SecurityConfiguration(RestAuthenticationEntryPoint restAuthenticationEntryPoint,
                                 RestAccessDeniedHandler restAccessDeniedHandler) {
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic(httpBasic -> httpBasic.authenticationEntryPoint(restAuthenticationEntryPoint))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions().disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/acct/payments").hasRole("ACCOUNTANT")
                        .requestMatchers(HttpMethod.PUT, "/api/acct/payments").hasRole("ACCOUNTANT")
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/api/acct/payments/**").hasRole("ACCOUNTANT")
                        .requestMatchers("/api/empl/payment", "/api/empl/payment/").hasAnyRole("USER", "ACCOUNTANT")
                        .requestMatchers("/api/feedback", "/api/feedback/**").permitAll()
                        .requestMatchers("/feedback", "/feedback/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(13);
    }

    @Bean
    public UserDetailsService userDetailsService(AccountUserRepository repository) {
        return username -> {
            String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
            return repository.findByEmailIgnoreCase(normalizedUsername)
                .map(this::buildUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        };
    }

    private org.springframework.security.core.userdetails.UserDetails buildUserDetails(AccountUser user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.name()))
                        .collect(Collectors.toSet()))
                .build();
    }
}
