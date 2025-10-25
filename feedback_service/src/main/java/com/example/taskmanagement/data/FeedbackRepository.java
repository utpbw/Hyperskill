package com.example.taskmanagement.data;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface FeedbackRepository extends MongoRepository<FeedbackDocument, String>, FeedbackRepositoryCustom {
}
