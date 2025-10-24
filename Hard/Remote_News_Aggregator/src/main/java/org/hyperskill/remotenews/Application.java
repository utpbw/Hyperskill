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
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
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

        private final HttpClient client;

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

                AtomicInteger failedCalls = new AtomicInteger();

                CompletableFuture<List<Transaction>> first = fetchTransactionsAsync(SERVER_ONE, account)
                        .exceptionally(ex -> {
                            failedCalls.incrementAndGet();
                            return List.of();
                        });

                CompletableFuture<List<Transaction>> second = fetchTransactionsAsync(SERVER_TWO, account)
                        .exceptionally(ex -> {
                            failedCalls.incrementAndGet();
                            return List.of();
                        });

                CompletableFuture.allOf(first, second).join();

                if (failedCalls.get() == 2) {
                    sendResponse(exchange, 500, toUtf8("Failed to fetch transactions"), TEXT_CONTENT_TYPE);
                    return;
                }

                List<Transaction> merged = Stream.of(first.join(), second.join())
                        .flatMap(List::stream)
                        .sorted(Comparator.comparing(
                                Transaction::timestampAsInstant,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                        .collect(Collectors.toList());

                byte[] responseBytes = OBJECT_MAPPER.writeValueAsBytes(merged);
                sendResponse(exchange, 200, responseBytes, JSON_CONTENT_TYPE);
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

        private CompletableFuture<List<Transaction>> fetchTransactionsAsync(String serverBaseUrl, String account) {
            URI requestUri = URI.create(serverBaseUrl + TRANSACTIONS_PATH + "?account=" + encode(account));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(requestUri)
                    .GET()
                    .build();

            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() != 200) {
                            throw new CompletionException(new IOException("Unexpected status " + response.statusCode()));
                        }

                        try {
                            return OBJECT_MAPPER.readValue(
                                    response.body(),
                                    new TypeReference<List<Transaction>>() { });
                        } catch (IOException parsingFailure) {
                            throw new CompletionException(parsingFailure);
                        }
                    });
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
}
