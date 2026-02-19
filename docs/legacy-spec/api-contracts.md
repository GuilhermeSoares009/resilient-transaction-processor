# API Contracts

## POST /api/v1/transactions
Request
```json
{
  "idempotencyKey": "abc-123",
  "accountId": "acc-1",
  "type": "DEBIT",
  "amount": 120.50,
  "currency": "BRL"
}
```

Response
```json
{
  "transactionId": "tx-1001",
  "status": "APPLIED",
  "balance": 879.50
}
```

## GET /api/v1/accounts/{id}/balance
Response
```json
{
  "accountId": "acc-1",
  "balance": 879.50,
  "currency": "BRL"
}
```

## GET /api/v1/ledger?accountId=acc-1
Response
```json
[
  {
    "entryId": "led-1",
    "transactionId": "tx-1001",
    "type": "DEBIT",
    "amount": 120.50,
    "createdAt": "2026-02-19T10:00:00Z"
  }
]
```
