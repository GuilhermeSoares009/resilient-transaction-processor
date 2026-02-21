package com.transaction.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

public final class TransactionProcessorApplication {
    private static final int DEFAULT_PORT = 8080;
    private static final int DEFAULT_RATE_LIMIT = 120;
    private static final long LATENCY_BUDGET_MS = 150;

    private final ObjectMapper mapper;
    private final RateLimiter rateLimiter;
    private final LedgerStore ledgerStore;

    private TransactionProcessorApplication(ObjectMapper mapper, RateLimiter rateLimiter, LedgerStore ledgerStore) {
        this.mapper = mapper;
        this.rateLimiter = rateLimiter;
        this.ledgerStore = ledgerStore;
    }

    public static void main(String[] args) throws IOException {
        int port = readIntEnv("PORT", DEFAULT_PORT);
        int rateLimit = readIntEnv("RATE_LIMIT_PER_MIN", DEFAULT_RATE_LIMIT);

        ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        RateLimiter limiter = new RateLimiter(rateLimit, Duration.ofMinutes(1));
        LedgerStore ledgerStore = new LedgerStore();
        TransactionProcessorApplication app = new TransactionProcessorApplication(mapper, limiter, ledgerStore);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/v1/health", app::handleHealth);
        server.createContext("/api/v1/transactions", app::handleTransaction);
        server.createContext("/api/v1/balance", app::handleBalance);
        server.createContext("/api/v1/ledger", app::handleLedger);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!allow(exchange)) {
            return;
        }
        writeJson(exchange, 200, new StatusResponse("ok"));
        log(exchange, new LogEntry("health check", newTraceId(), "", "", "ok", 200, LATENCY_BUDGET_MS, 0, false));
    }

    private void handleTransaction(HttpExchange exchange) throws IOException {
        Instant start = Instant.now();
        String traceId = newTraceId();

        if (!allow(exchange)) {
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, new ErrorResponse("method not allowed"));
            log(exchange, new LogEntry("method not allowed", traceId, "", "", "", 405, LATENCY_BUDGET_MS, 0, false));
            return;
        }

        TransactionRequest request;
        try {
            request = mapper.readValue(exchange.getRequestBody(), TransactionRequest.class);
        } catch (JsonProcessingException ex) {
            writeJson(exchange, 400, new ErrorResponse("invalid json"));
            log(exchange, new LogEntry("invalid request", traceId, "", "", "", 400, LATENCY_BUDGET_MS, 0, false));
            return;
        }

        List<String> errors = validate(request);
        if (!errors.isEmpty()) {
            writeJson(exchange, 400, new ErrorResponse(String.join("; ", errors)));
            log(exchange, new LogEntry("validation failed", traceId, request.transactionId, request.accountId, "", 400, LATENCY_BUDGET_MS, 0, false));
            return;
        }

        TransactionResult result = ledgerStore.apply(request);
        TransactionResponse response = new TransactionResponse(
            result.transactionId,
            traceId,
            result.status,
            result.balance,
            result.reason
        );
        writeJson(exchange, result.httpStatus, response);

        long durationMs = Duration.between(start, Instant.now()).toMillis();
        log(exchange, new LogEntry(
            "transaction processed",
            traceId,
            result.transactionId,
            request.accountId,
            result.status,
            result.httpStatus,
            LATENCY_BUDGET_MS,
            durationMs,
            durationMs > LATENCY_BUDGET_MS
        ));
    }

    private void handleBalance(HttpExchange exchange) throws IOException {
        if (!allow(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, new ErrorResponse("method not allowed"));
            return;
        }
        String accountId = queryParam(exchange, "accountId");
        if (accountId == null || accountId.isBlank()) {
            writeJson(exchange, 400, new ErrorResponse("accountId is required"));
            return;
        }
        double balance = ledgerStore.balance(accountId);
        writeJson(exchange, 200, new BalanceResponse(accountId, balance));
    }

    private void handleLedger(HttpExchange exchange) throws IOException {
        if (!allow(exchange)) {
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, new ErrorResponse("method not allowed"));
            return;
        }
        String accountId = queryParam(exchange, "accountId");
        if (accountId == null || accountId.isBlank()) {
            writeJson(exchange, 400, new ErrorResponse("accountId is required"));
            return;
        }
        writeJson(exchange, 200, new LedgerResponse(ledgerStore.ledger(accountId)));
    }

    private boolean allow(HttpExchange exchange) throws IOException {
        String key = clientIp(exchange);
        if (!rateLimiter.allow(key)) {
            writeJson(exchange, 429, new ErrorResponse("rate limit exceeded"));
            return false;
        }
        return true;
    }

    private void writeJson(HttpExchange exchange, int status, Object payload) throws IOException {
        byte[] data = mapper.writeValueAsBytes(payload);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private void log(HttpExchange exchange, LogEntry entry) throws IOException {
        byte[] data = mapper.writeValueAsBytes(entry);
        System.out.println(new String(data));
    }

    private List<String> validate(TransactionRequest request) {
        List<String> errors = new ArrayList<>();
        if (request.transactionId == null || request.transactionId.isBlank()) {
            errors.add("transactionId is required");
        }
        if (request.accountId == null || request.accountId.isBlank()) {
            errors.add("accountId is required");
        }
        if (request.amount <= 0) {
            errors.add("amount must be > 0");
        }
        if (request.type == null || request.type.isBlank()) {
            errors.add("type is required");
        }
        if (request.idempotencyKey == null || request.idempotencyKey.isBlank()) {
            errors.add("idempotencyKey is required");
        }
        return errors;
    }

    private String clientIp(HttpExchange exchange) {
        String forwarded = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = exchange.getRequestHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    private String queryParam(HttpExchange exchange, String key) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null || query.isBlank()) {
            return null;
        }
        String[] parts = query.split("&");
        for (String part : parts) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && key.equals(pair[0])) {
                return pair[1];
            }
        }
        return null;
    }

    private static String newTraceId() {
        return UUID.randomUUID().toString();
    }

    private static int readIntEnv(String key, int fallback) {
        String raw = System.getenv(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private record StatusResponse(String status) {}

    private record ErrorResponse(String error) {}

    private record TransactionRequest(
        String transactionId,
        String accountId,
        String type,
        double amount,
        String idempotencyKey
    ) {}

    private record TransactionResponse(
        String transactionId,
        String traceId,
        String status,
        double balance,
        String reason
    ) {}

    private record BalanceResponse(String accountId, double balance) {}

    private record LedgerEntry(
        String transactionId,
        String type,
        double amount,
        double balanceAfter,
        Instant createdAt
    ) {}

    private record LedgerResponse(List<LedgerEntry> entries) {}

    private record LogEntry(
        String message,
        String traceId,
        String transactionId,
        String accountId,
        String status,
        int httpStatus,
        long budgetMs,
        long durationMs,
        boolean budgetExceeded
    ) {}

    private static final class TransactionResult {
        private final String transactionId;
        private final String status;
        private final double balance;
        private final String reason;
        private final int httpStatus;

        private TransactionResult(String transactionId, String status, double balance, String reason, int httpStatus) {
            this.transactionId = transactionId;
            this.status = status;
            this.balance = balance;
            this.reason = reason;
            this.httpStatus = httpStatus;
        }
    }

    private static final class LedgerStore {
        private final Map<String, Double> balances = new ConcurrentHashMap<>();
        private final Map<String, TransactionResult> idempotency = new ConcurrentHashMap<>();
        private final Map<String, CopyOnWriteArrayList<LedgerEntry>> ledger = new ConcurrentHashMap<>();

        private TransactionResult apply(TransactionRequest request) {
            TransactionResult cached = idempotency.get(request.idempotencyKey);
            if (cached != null) {
                return cached;
            }

            synchronized (this) {
                TransactionResult existing = idempotency.get(request.idempotencyKey);
                if (existing != null) {
                    return existing;
                }

                double current = balances.getOrDefault(request.accountId, 0.0);
                double updated = current;
                String type = request.type.toLowerCase();
                if ("debit".equals(type)) {
                    if (current < request.amount) {
                        TransactionResult rejected = new TransactionResult(
                            request.transactionId,
                            "rejected",
                            current,
                            "insufficient-funds",
                            409
                        );
                        idempotency.put(request.idempotencyKey, rejected);
                        return rejected;
                    }
                    updated = current - request.amount;
                } else if ("credit".equals(type)) {
                    updated = current + request.amount;
                } else {
                    TransactionResult rejected = new TransactionResult(
                        request.transactionId,
                        "rejected",
                        current,
                        "invalid-transaction-type",
                        400
                    );
                    idempotency.put(request.idempotencyKey, rejected);
                    return rejected;
                }

                balances.put(request.accountId, updated);
                ledger.computeIfAbsent(request.accountId, ignored -> new CopyOnWriteArrayList<>())
                    .add(new LedgerEntry(request.transactionId, type, request.amount, updated, Instant.now()));

                TransactionResult applied = new TransactionResult(
                    request.transactionId,
                    "applied",
                    updated,
                    "applied",
                    200
                );
                idempotency.put(request.idempotencyKey, applied);
                return applied;
            }
        }

        private double balance(String accountId) {
            return balances.getOrDefault(accountId, 0.0);
        }

        private List<LedgerEntry> ledger(String accountId) {
            return List.copyOf(ledger.getOrDefault(accountId, new CopyOnWriteArrayList<>()));
        }
    }

    private static final class RateLimiter {
        private final int maxRequests;
        private final Duration window;
        private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

        private RateLimiter(int maxRequests, Duration window) {
            this.maxRequests = maxRequests;
            this.window = window;
        }

        private boolean allow(String key) {
            Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket());
            synchronized (bucket) {
                Instant now = Instant.now();
                if (bucket.windowStart == null || Duration.between(bucket.windowStart, now).compareTo(window) >= 0) {
                    bucket.windowStart = now;
                    bucket.count = 0;
                }
                if (bucket.count >= maxRequests) {
                    return false;
                }
                bucket.count++;
                return true;
            }
        }

        private static final class Bucket {
            private Instant windowStart;
            private int count;
        }
    }
}
