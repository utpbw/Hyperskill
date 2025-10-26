package com.hyperskill.tracker;

public record TrackerRecord(
    long id,
    String application,
    String username,
    String activity,
    int duration,
    int calories
) {
}
