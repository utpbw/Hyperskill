package com.hyperskill.tracker;

public record TrackerRecordRequest(String username, String activity, int duration, int calories) {
}
