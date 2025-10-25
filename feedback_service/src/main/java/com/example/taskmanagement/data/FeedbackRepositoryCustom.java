package com.example.taskmanagement.data;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FeedbackRepositoryCustom {
    Page<FeedbackDocument> findFiltered(
            Integer rating,
            String customer,
            String product,
            String vendor,
            Pageable pageable
    );
}
