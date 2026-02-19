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

- Inclui `docker-compose.yml` e Dockerfile(s).
- Healthcheck: http://localhost:8080/api/v1/health

## Qualidade (pre-commit)
Este repositorio usa pre-commit para CR + auditoria + anti-slop antes de cada commit.

```bash
pip install pre-commit
pre-commit install
```

Para rodar manualmente:

```bash
pre-commit run --all-files
```
