# Resilient Transaction Processor

Core de transacoes financeiras com idempotencia, controle de concorrencia e ledger imutavel para auditoria.

## Capacidades-chave
- Processamento de debito/credito com idempotencia
- Atualizacao de saldo ACID com controle seguro de concorrencia
- Ledger imutavel e publicacao de eventos
- Logs estruturados e tracing com OpenTelemetry

## Inicio rapido (Docker)
```bash
docker compose up --build
```

- Healthcheck: http://localhost:8080/api/v1/health

## Contratos de API
- OpenAPI: docs/api/openapi.yaml

## Documentacao
- Project Reference Guide: PROJECT_REFERENCE_GUIDE.md
- Especificacoes: docs/specs/spec.md
- Plano tecnico: docs/specs/plan.md
- Tarefas: docs/specs/tasks.md
- ADRs: docs/adr/
- Trade-offs: docs/trade-offs.md
- Threat model: docs/threat-model.md
- Performance budget: docs/performance-budget.md
- Feature flags: docs/feature-flags.md
- Legacy spec (arquivado): docs/legacy-spec/
