# Threat Model

## Ativos
- Saldo de contas
- Ledger imutavel
- Chaves de idempotencia

## Atores
- Usuario autenticado
- Atacante externo

## Ameacas
- Duplicidade de transacoes
- Replay de requests
- Escalada de privilegios

## Mitigacoes
- Idempotency keys
- Rate limiting
- Auditoria e logs estruturados
