package com.example.feedback.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FeedbackControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createFeedbackAndRetrieveById_returnsCreatedDocument() {
        FeedbackRequest request = new FeedbackRequest(
                4,
                "good but expensive",
                "John Doe",
                "MacBook Air",
                "Online Trade LLC"
        );

        ResponseEntity<Void> createResponse = restTemplate.postForEntity("/feedback", request, Void.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getHeaders().getLocation()).isNotNull();

        URI location = createResponse.getHeaders().getLocation();
        ResponseEntity<FeedbackResponse> getResponse = restTemplate.getForEntity(location, FeedbackResponse.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        FeedbackResponse body = getResponse.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo(extractId(location));
        assertThat(body.rating()).isEqualTo(4);
        assertThat(body.feedback()).isEqualTo("good but expensive");
        assertThat(body.customer()).isEqualTo("John Doe");
        assertThat(body.product()).isEqualTo("MacBook Air");
        assertThat(body.vendor()).isEqualTo("Online Trade LLC");
    }

    @Test
    void listFeedback_returnsNewestFirstWithNullOptionalFields() {
        FeedbackRequest firstRequest = new FeedbackRequest(
                2,
                null,
                null,
                "Blue duct tape",
                "99 Cents & Co."
        );
        FeedbackRequest secondRequest = new FeedbackRequest(
                5,
                "Fantastic",
                "Alice",
                "MacBook Air",
                "Online Trade LLC"
        );

        URI firstLocation = restTemplate.postForLocation("/feedback", firstRequest);
        URI secondLocation = restTemplate.postForLocation("/feedback", secondRequest);

        ResponseEntity<FeedbackResponse[]> listResponse = restTemplate.getForEntity("/feedback", FeedbackResponse[].class);

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        FeedbackResponse[] responses = listResponse.getBody();
        assertThat(responses).isNotNull();
        assertThat(responses).hasSizeGreaterThanOrEqualTo(2);
        assertThat(responses[0].id()).isEqualTo(extractId(secondLocation));
        assertThat(responses[0].feedback()).isEqualTo("Fantastic");
        assertThat(responses[0].customer()).isEqualTo("Alice");
        assertThat(responses[1].id()).isEqualTo(extractId(firstLocation));
        assertThat(responses[1].feedback()).isNull();
        assertThat(responses[1].customer()).isNull();
    }

    @Test
    void getFeedback_whenIdDoesNotExist_returnsNotFound() {
        ResponseEntity<FeedbackResponse> response = restTemplate.getForEntity(
                "/feedback/655e0c5f76a1e10ce2159b90",
                FeedbackResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private String extractId(URI location) {
        assertThat(location).isNotNull();
        String path = location.getPath();
        int lastSlashIndex = path.lastIndexOf('/');
        return lastSlashIndex >= 0 ? path.substring(lastSlashIndex + 1) : path;
    }
}
