package com.example.feedback.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FeedbackControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private com.example.feedback.data.FeedbackRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

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
    void listFeedback_returnsPagedResponseWithNewestFirst() {
        FeedbackRequest firstRequest = new FeedbackRequest(
                4,
                "good but expensive",
                "John Doe",
                "MacBook Air",
                "Online Trade LLC"
        );
        FeedbackRequest secondRequest = new FeedbackRequest(
                4,
                null,
                null,
                "Blue duct tape",
                "99 Cents & Co."
        );

        URI firstLocation = restTemplate.postForLocation("/feedback", firstRequest);
        URI secondLocation = restTemplate.postForLocation("/feedback", secondRequest);

        ResponseEntity<FeedbackPageResponse> listResponse = restTemplate.getForEntity(
                "/feedback",
                FeedbackPageResponse.class
        );

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        FeedbackPageResponse page = listResponse.getBody();
        assertThat(page).isNotNull();
        assertThat(page.totalDocuments()).isEqualTo(2);
        assertThat(page.firstPage()).isTrue();
        assertThat(page.lastPage()).isTrue();
        assertThat(page.documents()).hasSize(2);
        assertThat(page.documents().get(0).id()).isEqualTo(extractId(secondLocation));
        assertThat(page.documents().get(0).rating()).isEqualTo(4);
        assertThat(page.documents().get(0).feedback()).isNull();
        assertThat(page.documents().get(0).customer()).isNull();
        assertThat(page.documents().get(0).product()).isEqualTo("Blue duct tape");
        assertThat(page.documents().get(0).vendor()).isEqualTo("99 Cents & Co.");

        assertThat(page.documents().get(1).id()).isEqualTo(extractId(firstLocation));
        assertThat(page.documents().get(1).rating()).isEqualTo(4);
        assertThat(page.documents().get(1).feedback()).isEqualTo("good but expensive");
        assertThat(page.documents().get(1).customer()).isEqualTo("John Doe");
        assertThat(page.documents().get(1).product()).isEqualTo("MacBook Air");
        assertThat(page.documents().get(1).vendor()).isEqualTo("Online Trade LLC");
    }

    @Test
    void getFeedback_whenIdDoesNotExist_returnsNotFound() {
        ResponseEntity<FeedbackResponse> response = restTemplate.getForEntity(
                "/feedback/655e0c5f76a1e10ce2159b90",
                FeedbackResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listFeedback_withPaginationParameters_returnsRequestedPage() {
        List<String> createdIds = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            FeedbackRequest request = new FeedbackRequest(
                    5,
                    "feedback " + i,
                    "customer " + i,
                    "Product " + i,
                    "Vendor " + i
            );
            URI location = restTemplate.postForLocation("/feedback", request);
            createdIds.add(extractId(location));
        }

        ResponseEntity<FeedbackPageResponse> response = restTemplate.getForEntity(
                "/feedback?page=2&perPage=5",
                FeedbackPageResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        FeedbackPageResponse page = response.getBody();
        assertThat(page).isNotNull();
        assertThat(page.totalDocuments()).isEqualTo(7);
        assertThat(page.firstPage()).isFalse();
        assertThat(page.lastPage()).isTrue();
        assertThat(page.documents()).hasSize(2);
        List<String> expectedOrder = new ArrayList<>(createdIds);
        java.util.Collections.reverse(expectedOrder);
        int offset = 5;
        int endExclusive = Math.min(expectedOrder.size(), offset + 5);
        List<String> expectedPageIds = expectedOrder.subList(offset, endExclusive);
        assertThat(page.documents().stream().map(FeedbackResponse::id))
                .containsExactlyElementsOf(expectedPageIds);
    }

    @Test
    void listFeedback_withOutOfRangeParameters_defaultsToFirstPageWithDefaultSize() {
        List<String> createdIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            FeedbackRequest request = new FeedbackRequest(
                    3,
                    null,
                    null,
                    "Product X" + i,
                    "Vendor X" + i
            );
            URI location = restTemplate.postForLocation("/feedback", request);
            createdIds.add(extractId(location));
        }

        ResponseEntity<FeedbackPageResponse> response = restTemplate.getForEntity(
                "/feedback?page=0&perPage=25",
                FeedbackPageResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        FeedbackPageResponse page = response.getBody();
        assertThat(page).isNotNull();
        assertThat(page.totalDocuments()).isEqualTo(3);
        assertThat(page.firstPage()).isTrue();
        assertThat(page.lastPage()).isTrue();
        assertThat(page.documents()).hasSize(3);
        assertThat(page.documents().get(0).id()).isEqualTo(createdIds.get(createdIds.size() - 1));
        assertThat(page.documents().get(1).id()).isEqualTo(createdIds.get(createdIds.size() - 2));
        assertThat(page.documents().get(2).id()).isEqualTo(createdIds.get(createdIds.size() - 3));
    }

    @Test
    void listFeedback_whenRequestingBeyondLastPage_returnsEmptyDocuments() {
        for (int i = 0; i < 3; i++) {
            FeedbackRequest request = new FeedbackRequest(
                    2,
                    null,
                    null,
                    "Product Y" + i,
                    "Vendor Y" + i
            );
            restTemplate.postForLocation("/feedback", request);
        }

        ResponseEntity<FeedbackPageResponse> response = restTemplate.getForEntity(
                "/feedback?page=5&perPage=5",
                FeedbackPageResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        FeedbackPageResponse page = response.getBody();
        assertThat(page).isNotNull();
        assertThat(page.totalDocuments()).isEqualTo(3);
        assertThat(page.documents()).isEmpty();
        assertThat(page.firstPage()).isFalse();
        assertThat(page.lastPage()).isTrue();
    }

    @Test
    void listFeedback_whenPerPageBelowMinimum_resetsToDefaultsAndReturnsFirstPage() {
        FeedbackRequest firstRequest = new FeedbackRequest(
                4,
                "good but expensive",
                "John Doe",
                "MacBook Air",
                "Online Trade LLC"
        );
        FeedbackRequest secondRequest = new FeedbackRequest(
                4,
                null,
                null,
                "Blue duct tape",
                "99 Cents & Co."
        );

        URI firstLocation = restTemplate.postForLocation("/feedback", firstRequest);
        URI secondLocation = restTemplate.postForLocation("/feedback", secondRequest);

        ResponseEntity<FeedbackPageResponse> response = restTemplate.getForEntity(
                "/feedback?page=2&perPage=3",
                FeedbackPageResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        FeedbackPageResponse page = response.getBody();
        assertThat(page).isNotNull();
        assertThat(page.totalDocuments()).isEqualTo(2);
        assertThat(page.firstPage()).isTrue();
        assertThat(page.lastPage()).isTrue();
        assertThat(page.documents()).hasSize(2);
        assertThat(page.documents().get(0).id()).isEqualTo(extractId(secondLocation));
        assertThat(page.documents().get(0).feedback()).isNull();
        assertThat(page.documents().get(0).customer()).isNull();
        assertThat(page.documents().get(0).product()).isEqualTo("Blue duct tape");
        assertThat(page.documents().get(0).vendor()).isEqualTo("99 Cents & Co.");
        assertThat(page.documents().get(1).id()).isEqualTo(extractId(firstLocation));
        assertThat(page.documents().get(1).feedback()).isEqualTo("good but expensive");
        assertThat(page.documents().get(1).customer()).isEqualTo("John Doe");
        assertThat(page.documents().get(1).product()).isEqualTo("MacBook Air");
        assertThat(page.documents().get(1).vendor()).isEqualTo("Online Trade LLC");
    }

    @Test
    void listFeedback_withMultipleFilters_appliesFiltersBeforePagination() {
        List<String> matchingIds = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            FeedbackRequest request = new FeedbackRequest(
                    5,
                    "excellent product " + i,
                    "Customer " + i,
                    "Blue duct tape",
                    "Total Sales"
            );
            URI location = restTemplate.postForLocation("/feedback", request);
            matchingIds.add(extractId(location));
        }

        restTemplate.postForLocation("/feedback", new FeedbackRequest(
                4,
                "different rating",
                "Customer 100",
                "Blue duct tape",
                "Total Sales"
        ));
        restTemplate.postForLocation("/feedback", new FeedbackRequest(
                5,
                "different product",
                "Customer 200",
                "MacBook Air",
                "Total Sales"
        ));

        URI requestUri = UriComponentsBuilder.fromPath("/feedback")
                .queryParam("page", 2)
                .queryParam("perPage", 5)
                .queryParam("rating", 5)
                .queryParam("product", "blue duct tape")
                .queryParam("vendor", "Total Sales")
                .build()
                .toUri();

        ResponseEntity<FeedbackPageResponse> response = restTemplate.getForEntity(
                requestUri,
                FeedbackPageResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        FeedbackPageResponse page = response.getBody();
        assertThat(page).isNotNull();
        assertThat(page.totalDocuments()).isEqualTo(6);
        assertThat(page.firstPage()).isFalse();
        assertThat(page.lastPage()).isTrue();
        assertThat(page.documents()).hasSize(1);

        List<String> expectedOrder = new ArrayList<>(matchingIds);
        java.util.Collections.reverse(expectedOrder);
        assertThat(page.documents().get(0).id()).isEqualTo(expectedOrder.get(5));
        assertThat(page.documents().get(0).product()).isEqualTo("Blue duct tape");
        assertThat(page.documents().get(0).vendor()).isEqualTo("Total Sales");
        assertThat(page.documents().get(0).rating()).isEqualTo(5);
    }

    @Test
    void listFeedback_withFiltersYieldingNoMatches_returnsEmptyResult() {
        restTemplate.postForLocation("/feedback", new FeedbackRequest(
                3,
                "average",
                "Chris",
                "Generic product",
                "General Store"
        ));

        ResponseEntity<FeedbackPageResponse> response = restTemplate.getForEntity(
                "/feedback?rating=5&customer=Alex",
                FeedbackPageResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        FeedbackPageResponse page = response.getBody();
        assertThat(page).isNotNull();
        assertThat(page.totalDocuments()).isZero();
        assertThat(page.firstPage()).isTrue();
        assertThat(page.lastPage()).isTrue();
        assertThat(page.documents()).isEmpty();
    }

    private String extractId(URI location) {
        assertThat(location).isNotNull();
        String path = location.getPath();
        int lastSlashIndex = path.lastIndexOf('/');
        return lastSlashIndex >= 0 ? path.substring(lastSlashIndex + 1) : path;
    }
}
