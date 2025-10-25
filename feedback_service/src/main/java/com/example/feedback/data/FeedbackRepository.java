package com.example.feedback.data;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data Mongo repository for feedback documents with additional custom queries.
 */
public interface FeedbackRepository extends MongoRepository<FeedbackDocument, String>, FeedbackRepositoryCustom {
}
