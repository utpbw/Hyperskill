package com.example.feedback.api;

import com.example.feedback.data.FeedbackDocument;
import com.example.feedback.data.FeedbackRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

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

    @GetMapping("/{id}")
    public ResponseEntity<FeedbackResponse> getFeedback(@PathVariable String id) {
        return repository.findById(id)
                .map(FeedbackResponse::fromDocument)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<FeedbackResponse> getAllFeedback() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "_id")).stream()
                .map(FeedbackResponse::fromDocument)
                .toList();
    }
}
