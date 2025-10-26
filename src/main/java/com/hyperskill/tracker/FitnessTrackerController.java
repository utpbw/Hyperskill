package com.hyperskill.tracker;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tracker")
public class FitnessTrackerController {

    private final AtomicLong idSequence = new AtomicLong(1L);
    private final Deque<TrackerRecord> records = new ConcurrentLinkedDeque<>();

    @PostMapping
    public ResponseEntity<TrackerRecord> createRecord(@RequestBody TrackerRecordRequest request) {
        TrackerRecord record = new TrackerRecord(
            idSequence.getAndIncrement(),
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
