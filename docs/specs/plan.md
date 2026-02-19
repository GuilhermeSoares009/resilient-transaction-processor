# Plan: Resilient Transaction Processor

## Architecture
- Single Spring Boot service with layered modules (api, application, domain, infra).
- PostgreSQL as source of truth for balances and ledger.
- Redis for read cache.
- Kafka for event publication.

## Data Model
- accounts(id, balance, version)
- transactions(id, idempotency_key, account_id, type, amount, status)
- ledger(id, transaction_id, account_id, type, amount, created_at)

## Consistency and Concurrency
- Optimistic locking on accounts with retries.
- Idempotency table keyed by idempotency_key.

## Observability
- Structured JSON logs with traceId, spanId, accountId, transactionId.
- OpenTelemetry tracing for transaction and ledger flows.

## Security
- Input validation and rate limiting on /transactions.
- No PII in logs.

## Feature Flags
- ledger_v2_enabled
- kafka_publish_enabled
- optimistic_retry_enabled

## Local Dev and CI
- Docker Compose for Postgres, Redis, Kafka.
- CI runs unit + integration tests.

## ADRs
- Immutable ledger separation
- Optimistic concurrency control
