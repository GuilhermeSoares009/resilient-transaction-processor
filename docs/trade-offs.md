# Trade-offs

- ACID vs eventual consistency: escolhido ACID para saldo, eventual para eventos
- Kafka vs fila simples: Kafka para garantir durabilidade e replay
- Lock pessimista vs otimista: otimista para throughput
- Ledger separado vs tabela unica: separado para auditoria
- Redis cache vs sem cache: Redis para leitura rapida
- Complexidade vs simplicidade: aceita complexidade para garantir confiabilidade
