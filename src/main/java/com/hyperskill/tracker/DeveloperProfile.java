package com.hyperskill.tracker;

import java.util.List;

public record DeveloperProfile(Long id, String email, List<DeveloperApplicationView> applications) {
}
