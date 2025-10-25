package com.example.taskmanagement.data;

import com.example.taskmanagement.api.FeedbackRequest;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "feedback")
public class FeedbackDocument {

    @Id
    private String id;
    private Integer rating;
    private String feedback;
    private String customer;
    private String product;
    private String vendor;

    public FeedbackDocument() {
    }

    public FeedbackDocument(Integer rating, String feedback, String customer, String product, String vendor) {
        this.rating = rating;
        this.feedback = feedback;
        this.customer = customer;
        this.product = product;
        this.vendor = vendor;
    }

    public static FeedbackDocument fromRequest(FeedbackRequest request) {
        return new FeedbackDocument(
                request.rating(),
                request.feedback(),
                request.customer(),
                request.product(),
                request.vendor()
        );
    }

    public String getId() {
        return id;
    }

    public Integer getRating() {
        return rating;
    }

    public String getFeedback() {
        return feedback;
    }

    public String getCustomer() {
        return customer;
    }

    public String getProduct() {
        return product;
    }

    public String getVendor() {
        return vendor;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }
}
