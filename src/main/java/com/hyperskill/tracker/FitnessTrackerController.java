package com.hyperskill.tracker;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracker")
public class FitnessTrackerController {

    private final ApplicationRepository applicationRepository;
    private final AtomicLong idSequence = new AtomicLong(1L);
    private final Deque<TrackerRecord> records = new ConcurrentLinkedDeque<>();
    private final TrackerRateLimiter trackerRateLimiter;

    public FitnessTrackerController(
        ApplicationRepository applicationRepository,
        TrackerRateLimiter trackerRateLimiter
    ) {
        this.applicationRepository = applicationRepository;
        this.trackerRateLimiter = trackerRateLimiter;
    }

    @PostMapping
    public ResponseEntity<TrackerRecord> createRecord(
        @RequestHeader(value = "X-API-Key", required = false) String apiKey,
        @RequestBody TrackerRecordRequest request
    ) {
        Optional<Application> application = authenticate(apiKey);
        if (application.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Application authenticatedApplication = application.get();
        if (!trackerRateLimiter.tryAcquire(authenticatedApplication)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        TrackerRecord record = new TrackerRecord(
            idSequence.getAndIncrement(),
            authenticatedApplication.getName(),
            request.username(),
            request.activity(),
            request.duration(),
            request.calories()
        );

        records.offerFirst(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    @GetMapping
    public ResponseEntity<List<TrackerRecord>> listRecords(
        @RequestHeader(value = "X-API-Key", required = false) String apiKey
    ) {
        Optional<Application> application = authenticate(apiKey);
        if (application.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!trackerRateLimiter.tryAcquire(application.get())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        return ResponseEntity.ok(new ArrayList<>(records));
    }

    private Optional<Application> authenticate(String apiKey) {
        if (apiKey == null) {
            return Optional.empty();
        }

        return applicationRepository.findByApiKey(apiKey);
    }
}
