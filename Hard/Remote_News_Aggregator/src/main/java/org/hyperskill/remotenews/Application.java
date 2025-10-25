package org.hyperskill.remotenews;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Application {
    private static final int PORT = 8080;
    private static final String SERVER_ONE = "http://localhost:8888";
    private static final String SERVER_TWO = "http://localhost:8889";
    private static final String TRANSACTIONS_PATH = "/transactions";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        HttpClient client = HttpClient.newHttpClient();

        server.createContext("/aggregate", new AggregateHandler(client));
        server.setExecutor(null);
        server.start();
    }

    private static class AggregateHandler implements HttpHandler {
        private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";
        private static final String TEXT_CONTENT_TYPE = "text/plain;charset=UTF-8";
        private static final int MAX_RETRIES = 5;

        private final HttpClient client;
        private final java.util.concurrent.ConcurrentMap<String, CompletableFuture<AggregateResult>> cache =
                new java.util.concurrent.ConcurrentHashMap<>();

        private AggregateHandler(HttpClient client) {
            this.client = client;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, null, null);
                    return;
                }

                String account = extractAccount(exchange.getRequestURI().getQuery());
                if (account == null || account.isBlank()) {
                    sendResponse(exchange, 400, toUtf8("Missing account parameter"), TEXT_CONTENT_TYPE);
                    return;
                }

                AggregateResult result = getOrLoadAggregate(account).join();

                if (!result.isSuccess()) {
                    byte[] message = toUtf8("Failed to fetch transactions");
                    sendResponse(exchange, result.statusCode(), message, TEXT_CONTENT_TYPE);
                    return;
                }

                sendResponse(exchange, 200, result.responseBody(), JSON_CONTENT_TYPE);
            } catch (IOException e) {
                sendResponse(exchange, 500, null, null);
            } catch (RuntimeException e) {
                Throwable cause = e.getCause();
                if (cause instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                sendResponse(exchange, 500, null, null);
            } finally {
                exchange.close();
            }
        }

        private CompletableFuture<AggregateResult> getOrLoadAggregate(String account) {
            CompletableFuture<AggregateResult> cached = cache.get(account);
            if (cached != null) {
                return cached;
            }

            CompletableFuture<AggregateResult> loader = computeAggregate(account);
            CompletableFuture<AggregateResult> existing = cache.putIfAbsent(account, loader);
            if (existing != null) {
                return existing;
            }

            loader.whenComplete((result, throwable) -> {
                if (throwable != null || result == null || !result.isCacheable()) {
                    cache.remove(account, loader);
                }
            });

            return loader;
        }

        private CompletableFuture<AggregateResult> computeAggregate(String account) {
            CompletableFuture<UpstreamOutcome> first = fetchTransactionsAsync(SERVER_ONE, account);
            CompletableFuture<UpstreamOutcome> second = fetchTransactionsAsync(SERVER_TWO, account);

            return CompletableFuture.allOf(first, second)
                    .thenApply(ignored -> combineResults(first.join(), second.join()))
                    .exceptionally(throwable -> AggregateResult.failure(500));
        }

        private AggregateResult combineResults(UpstreamOutcome firstResult, UpstreamOutcome secondResult) {
            boolean firstSuccess = firstResult.isSuccess();
            boolean secondSuccess = secondResult.isSuccess();

            if (!firstSuccess && !secondSuccess) {
                return AggregateResult.failure(firstResult.statusOrDefault(secondResult));
            }

            List<Transaction> merged = Stream.of(firstResult, secondResult)
                    .filter(UpstreamOutcome::isSuccess)
                    .flatMap(outcome -> outcome.transactions.stream())
                    .sorted(Comparator.comparing(
                            Transaction::timestampAsInstant,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());

            try {
                byte[] responseBytes = OBJECT_MAPPER.writeValueAsBytes(merged);
                return AggregateResult.success(responseBytes, firstSuccess && secondSuccess);
            } catch (IOException e) {
                return AggregateResult.failure(500);
            }
        }

        private CompletableFuture<UpstreamOutcome> fetchTransactionsAsync(String serverBaseUrl, String account) {
            String encodedAccount = encode(account);
            return fetchTransactionsWithRetries(serverBaseUrl, encodedAccount, MAX_RETRIES);
        }

        private CompletableFuture<UpstreamOutcome> fetchTransactionsWithRetries(
                String serverBaseUrl,
                String encodedAccount,
                int attemptsRemaining
        ) {
            URI requestUri = URI.create(serverBaseUrl + TRANSACTIONS_PATH + "?account=" + encodedAccount);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(requestUri)
                    .GET()
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenCompose(response -> handleResponse(response, serverBaseUrl, encodedAccount, attemptsRemaining))
                    .exceptionally(throwable -> UpstreamOutcome.failure(null, throwable));
        }

        private CompletableFuture<UpstreamOutcome> handleResponse(
                HttpResponse<String> response,
                String serverBaseUrl,
                String encodedAccount,
                int attemptsRemaining
        ) {
            int status = response.statusCode();
            if (status == 200) {
                try {
                    List<Transaction> transactions = OBJECT_MAPPER.readValue(
                            response.body(),
                            new TypeReference<List<Transaction>>() { }
                    );
                    return CompletableFuture.completedFuture(UpstreamOutcome.success(transactions));
                } catch (IOException parsingFailure) {
                    return CompletableFuture.failedFuture(parsingFailure);
                }
            }

            if (shouldRetry(status) && attemptsRemaining > 1) {
                return fetchTransactionsWithRetries(serverBaseUrl, encodedAccount, attemptsRemaining - 1);
            }

            return CompletableFuture.completedFuture(UpstreamOutcome.failure(status, null));
        }

        private boolean shouldRetry(int status) {
            return status == 503 || status == 529;
        }

        private byte[] toUtf8(String value) {
            return value.getBytes(StandardCharsets.UTF_8);
        }

        private String encode(String value) {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        }

        private void sendResponse(HttpExchange exchange, int statusCode, byte[] body, String contentType) throws IOException {
            if (contentType != null) {
                exchange.getResponseHeaders().set("Content-Type", contentType);
            }

            if (body == null) {
                exchange.sendResponseHeaders(statusCode, -1);
                return;
            }

            exchange.sendResponseHeaders(statusCode, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }

        private String extractAccount(String query) {
            if (query == null || query.isBlank()) {
                return null;
            }

            Map<String, String> params = parseQuery(query);
            return params.get("account");
        }

        private Map<String, String> parseQuery(String query) {
            return java.util.Arrays.stream(query.split("&"))
                    .map(pair -> pair.split("=", 2))
                    .filter(parts -> parts.length > 0 && !parts[0].isEmpty())
                    .collect(Collectors.toMap(
                            parts -> decode(parts[0]),
                            parts -> parts.length > 1 ? decode(parts[1]) : "",
                            (first, second) -> second
                    ));
        }

        private String decode(String value) {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
    }

    public static class Transaction {
        private String id;
        private String serverId;
        private String account;
        private String amount;
        private String timestamp;

        public Transaction() {
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getServerId() {
            return serverId;
        }

        public void setServerId(String serverId) {
            this.serverId = serverId;
        }

        public String getAccount() {
            return account;
        }

        public void setAccount(String account) {
            this.account = account;
        }

        public String getAmount() {
            return amount;
        }

        public void setAmount(String amount) {
            this.amount = amount;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        private Instant timestampAsInstant() {
            if (timestamp == null) {
                return null;
            }

            try {
                return Instant.parse(timestamp);
            } catch (DateTimeParseException e) {
                return null;
            }
        }
    }

    private static final class AggregateResult {
        private final byte[] responseBody;
        private final Integer statusCode;
        private final boolean cacheable;

        private AggregateResult(byte[] responseBody, Integer statusCode, boolean cacheable) {
            this.responseBody = responseBody;
            this.statusCode = statusCode;
            this.cacheable = cacheable;
        }

        private static AggregateResult success(byte[] responseBody, boolean cacheable) {
            return new AggregateResult(responseBody, null, cacheable);
        }

        private static AggregateResult failure(int statusCode) {
            return new AggregateResult(null, statusCode, false);
        }

        private boolean isSuccess() {
            return statusCode == null;
        }

        private boolean isCacheable() {
            return cacheable && isSuccess();
        }

        private byte[] responseBody() {
            return responseBody;
        }

        private int statusCode() {
            return statusCode == null ? 200 : statusCode;
        }
    }

    private static final class UpstreamOutcome {
        private final List<Transaction> transactions;
        private final Integer failureStatus;
        private final Throwable error;

        private UpstreamOutcome(List<Transaction> transactions, Integer failureStatus, Throwable error) {
            this.transactions = transactions == null ? List.of() : transactions;
            this.failureStatus = failureStatus;
            this.error = error;
        }

        private static UpstreamOutcome success(List<Transaction> transactions) {
            return new UpstreamOutcome(transactions, null, null);
        }

        private static UpstreamOutcome failure(Integer status, Throwable error) {
            return new UpstreamOutcome(List.of(), status, error);
        }

        private boolean isSuccess() {
            return failureStatus == null && error == null;
        }

        private int statusOrDefault(UpstreamOutcome alternative) {
            Integer preferred = preferredStatus(failureStatus, alternative.failureStatus);
            return preferred != null ? preferred : 500;
        }

        private static Integer preferredStatus(Integer first, Integer second) {
            if (Integer.valueOf(529).equals(first) || Integer.valueOf(529).equals(second)) {
                return 529;
            }
            if (Integer.valueOf(503).equals(first) || Integer.valueOf(503).equals(second)) {
                return 503;
            }
            if (first != null) {
                return first;
            }
            return second;
        }
    }
}
