package com.example.feedback.api;

import com.example.feedback.data.FeedbackDocument;
import com.example.feedback.data.FeedbackRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackRepository repository;

    public FeedbackController(FeedbackRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Void> createFeedback(@Valid @RequestBody FeedbackRequest request) {
        FeedbackDocument document = FeedbackDocument.fromRequest(request);
        FeedbackDocument saved = repository.save(document);
        URI location = URI.create("/feedback/" + saved.getId());
        return ResponseEntity.created(location).build();
    }
}
