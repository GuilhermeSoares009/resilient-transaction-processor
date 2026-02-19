# AGENTS.md

## Setup commands
- Install deps: `./mvnw -q -DskipTests package`
- Start dev server: `./mvnw spring-boot:run`
- Run tests: `./mvnw test`

## Code style
- Java 21, Spring Boot 3
- Constructor injection, evitar field injection
- Logs estruturados JSON
- Nao usar reflection sem justificativa

## Arquitetura
- Camadas: api, application, domain, infrastructure
- API versionada em /api/v1
- Eventos publicados via Kafka
- Ledger imutavel separado

## Padrões de logging
- JSON com traceId, spanId, accountId, transactionId
- Nivel INFO para fluxo normal, WARN para degradacao

## Estrategia de testes
- Unitarios para regras de negocio
- Integracao com Testcontainers
- Contratos para endpoints criticos

## Regras de seguranca
- Validar input e idempotency key
- Rate limiting em endpoints criticos
- Sem PII em logs

## Checklist de PR
- Testes passam localmente
- Lint/format ok
- ADR atualizado quando necessario
- Docs atualizadas (README, spec)

## Diretrizes de performance
- p95 /transactions < 150ms
- Limite de payload 256KB
- Backpressure com timeouts
