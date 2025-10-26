package com.hyperskill.tracker;

public record TrackerRecord(
    long id,
    String publisher,
    String username,
    String activity,
    int duration,
    int calories
) {
}
