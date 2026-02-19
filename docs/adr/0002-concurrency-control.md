# ADR 0002 - Controle de Concorrencia com Lock Otimista

## Status
Aceito

## Contexto
Concorrencia pode causar race conditions ao atualizar saldos.

## Decisao
Usar lock otimista com versao em conta e retry controlado.

## Consequencias
- Menos bloqueio em alta concorrencia
- Possivel retry e aumento de latencia em picos
