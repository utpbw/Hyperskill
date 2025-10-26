package com.hyperskill.tracker;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
public class TrackerRateLimiter {

    private static final long MIN_INTERVAL_MILLIS = 1_000L;

    private final ConcurrentMap<Long, AtomicLong> lastRequestTimes = new ConcurrentHashMap<>();

    public boolean tryAcquire(Application application) {
        if (!isBasicCategory(application)) {
            return true;
        }

        long now = System.currentTimeMillis();
        AtomicLong lastRequest = lastRequestTimes.computeIfAbsent(
            application.getId(),
            ignored -> new AtomicLong(0L)
        );

        while (true) {
            long previous = lastRequest.get();
            if (previous > 0L && now - previous < MIN_INTERVAL_MILLIS) {
                return false;
            }

            if (lastRequest.compareAndSet(previous, now)) {
                return true;
            }
        }
    }

    private boolean isBasicCategory(Application application) {
        return "basic".equalsIgnoreCase(application.getCategory());
    }
}
