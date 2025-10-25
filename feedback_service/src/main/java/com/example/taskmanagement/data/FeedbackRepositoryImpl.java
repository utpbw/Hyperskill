package com.example.taskmanagement.data;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;
import java.util.regex.Pattern;

public class FeedbackRepositoryImpl implements FeedbackRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    public FeedbackRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Page<FeedbackDocument> findFiltered(
            Integer rating,
            String customer,
            String product,
            String vendor,
            Pageable pageable
    ) {
        Query baseQuery = buildQuery(rating, customer, product, vendor);
        Query pageQuery = buildQuery(rating, customer, product, vendor);
        pageQuery.with(pageable);

        List<FeedbackDocument> documents = mongoTemplate.find(pageQuery, FeedbackDocument.class);

        long total = mongoTemplate.count(baseQuery, FeedbackDocument.class);

        return PageableExecutionUtils.getPage(
                documents,
                pageable,
                () -> total
        );
    }

    private Query buildQuery(Integer rating, String customer, String product, String vendor) {
        Query query = new Query();

        if (rating != null) {
            query.addCriteria(Criteria.where("rating").is(rating));
        }
        if (customer != null) {
            query.addCriteria(caseInsensitiveEquals("customer", customer));
        }
        if (product != null) {
            query.addCriteria(caseInsensitiveEquals("product", product));
        }
        if (vendor != null) {
            query.addCriteria(caseInsensitiveEquals("vendor", vendor));
        }

        return query;
    }

    private Criteria caseInsensitiveEquals(String field, String value) {
        return Criteria.where(field).regex("^" + Pattern.quote(value) + "$", "i");
    }
}
