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

    public FitnessTrackerController(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @PostMapping
    public ResponseEntity<TrackerRecord> createRecord(
        @RequestHeader(value = "X-API-Key", required = false) String apiKey,
        @RequestBody TrackerRecordRequest request
    ) {
        if (apiKey == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Application> application = applicationRepository.findByApiKey(apiKey);
        if (application.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TrackerRecord record = new TrackerRecord(
            idSequence.getAndIncrement(),
            application.get().getName(),
            request.username(),
            request.activity(),
            request.duration(),
            request.calories()
        );

        records.offerFirst(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    @GetMapping
    public List<TrackerRecord> listRecords() {
        return new ArrayList<>(records);
    }
}
