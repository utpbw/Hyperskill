package com.example.feedback.data;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface FeedbackRepository extends MongoRepository<FeedbackDocument, String>, FeedbackRepositoryCustom {
}
