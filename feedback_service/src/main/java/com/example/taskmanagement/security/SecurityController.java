package com.example.taskmanagement.security;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/security", produces = MediaType.APPLICATION_JSON_VALUE)
public class SecurityController {

    private final SecurityEventService securityEventService;

    public SecurityController(SecurityEventService securityEventService) {
        this.securityEventService = securityEventService;
    }

    @GetMapping("/events")
    public List<SecurityEventResponse> findAllEvents() {
        return securityEventService.findAll();
    }
}
