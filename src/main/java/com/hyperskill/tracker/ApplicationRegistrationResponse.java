package com.hyperskill.tracker;

public record ApplicationRegistrationResponse(
    String name,
    String apikey,
    String category
) {
}
