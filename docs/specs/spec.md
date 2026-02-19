# Spec: Resilient Transaction Processor (MVP)

## Product Vision
- Deliver reliable debit/credit processing with strong balance consistency.
- Prevent duplicate processing via idempotency keys.
- Provide an immutable ledger for audit and traceability.

## User Scenarios and Testing

### User Story 1 - Process a transaction safely (Priority: P1)
As a platform engineer, I want to submit a debit or credit once and get a stable result so balances stay consistent even with retries.

**Why this priority**: Core value is safe transaction processing.

**Independent Test**: Submit the same transaction twice and confirm no duplicate balance change.

**Acceptance Scenarios**:
1. **Given** an account with balance 100, **When** a debit of 40 is processed, **Then** balance becomes 60 and a ledger entry is created.
2. **Given** a transaction with idempotency key K was processed, **When** the same request is retried with K, **Then** the response is the same and no duplicate ledger entry is created.

---

### User Story 2 - Query balances and ledger (Priority: P2)
As an auditor, I want to query the balance and ledger to trace all transactions.

**Why this priority**: Auditability is essential for financial systems.

**Independent Test**: Create two transactions and verify ledger query returns two entries in order.

**Acceptance Scenarios**:
1. **Given** an account with transactions, **When** I request /balance, **Then** I see the current balance and currency.
2. **Given** an account with transactions, **When** I request /ledger, **Then** I see immutable entries with transaction IDs and timestamps.

---

### User Story 3 - Publish events for downstream consumers (Priority: P3)
As a downstream service owner, I want to receive transaction events to update projections.

**Why this priority**: Eventing enables integrations and consistency across systems.

**Independent Test**: Process a transaction and verify an event is published to the configured topic.

**Acceptance Scenarios**:
1. **Given** a successful transaction, **When** it is committed, **Then** an event is published.

### Edge Cases
- Duplicate idempotency key with different payload.
- Concurrent debits on the same account.
- Ledger query for non-existent account.

## Functional Requirements
- FR-01: Create account with initial balance.
- FR-02: Process debit/credit with idempotency key.
- FR-03: Enforce safe concurrent updates to balances.
- FR-04: Query account balance.
- FR-05: Query immutable ledger by account.
- FR-06: Publish transaction events for downstream consumers.
- FR-07: Expose healthcheck at /api/v1/health.

## Non-Functional Requirements
- NFR-01: p95 /api/v1/transactions < 150ms in local env.
- NFR-02: Balance updates are ACID.
- NFR-03: Structured JSON logs with traceId and transactionId.
- NFR-04: OpenTelemetry traces for transaction and ledger flows.
- NFR-05: 100% Docker local environment.
- NFR-06: API versioned under /api/v1.

## Success Criteria
- SC-01: 0 duplicate ledger entries when submitting the same idempotency key.
- SC-02: p95 latency for /transactions remains under 150ms in local tests.
- SC-03: Ledger query returns immutable, ordered entries with timestamps.

## API Contracts
- OpenAPI: .specify/specs/001-core-transactions/contracts/openapi.yaml

## Roadmap
- Milestone 1: Account + transaction MVP.
- Milestone 2: Idempotency + concurrency control.
- Milestone 3: Ledger + Kafka events.
- Milestone 4: Observability + security hardening.

## Trade-offs
- ACID vs eventual consistency.
- Kafka vs internal queue.
- Pessimistic vs optimistic locking.
- Separate ledger vs single table.
- Redis cache vs no cache.
- Complexity vs simplicity.
