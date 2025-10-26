package com.hyperskill.tracker;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DeveloperDetailsService implements UserDetailsService {

    private final DeveloperRepository developerRepository;

    public DeveloperDetailsService(DeveloperRepository developerRepository) {
        this.developerRepository = developerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Developer developer = developerRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("Developer not found"));

        return User.withUsername(developer.getEmail())
            .password(developer.getPassword())
            .roles("DEVELOPER")
            .build();
    }
}
