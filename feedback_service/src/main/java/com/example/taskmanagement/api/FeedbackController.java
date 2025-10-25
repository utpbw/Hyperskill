package com.example.taskmanagement.api;

import com.example.taskmanagement.data.FeedbackDocument;
import com.example.taskmanagement.data.FeedbackRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
    public FeedbackPageResponse getAllFeedback(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "perPage", required = false) Integer perPage,
            @RequestParam(value = "rating", required = false) Integer rating,
            @RequestParam(value = "customer", required = false) String customer,
            @RequestParam(value = "product", required = false) String product,
            @RequestParam(value = "vendor", required = false) String vendor
    ) {
        int sanitizedPage = page == null ? 1 : page;
        int sanitizedPerPage = perPage == null ? 10 : perPage;

        if (sanitizedPerPage < 5 || sanitizedPerPage > 20) {
            sanitizedPerPage = 10;
            sanitizedPage = 1;
        }

        if (sanitizedPage < 1) {
            sanitizedPage = 1;
        }

        Pageable pageable = PageRequest.of(
                sanitizedPage - 1,
                sanitizedPerPage,
                Sort.by(Sort.Direction.DESC, "_id")
        );

        Page<FeedbackDocument> feedbackPage = repository.findFiltered(
                rating,
                trimToNull(customer),
                trimToNull(product),
                trimToNull(vendor),
                pageable
        );
        List<FeedbackResponse> documents = feedbackPage.getContent().stream()
                .map(FeedbackResponse::fromDocument)
                .toList();

        return new FeedbackPageResponse(
                feedbackPage.getTotalElements(),
                feedbackPage.isFirst(),
                feedbackPage.isLast(),
                documents
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
