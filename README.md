# Transactions API

A Spring Boot REST API for managing financial transactions. Supports creating, updating, deleting, filtering, and bulk payment processing of transactions.

---
## 📦 Technologies

- Java 21+
- Spring Boot
- Spring Data JPA
- H2
- Maven
- JUnit & MockMvc for testing
---

### 🚀 Running the App
Accessed in http://localhost:8085/api/transactions

Will run in port 8085 (can be changed in application.properties)
```bash
mvn spring-boot:run
```

### 🧾 Running tests
```bash
mvn test
```
## 🧠 Overview

This API handles basic financial transactions and supports:

- Creating transactions
- Filtering and paginating transactions
- Updating/deleting (only PENDING status)
- Paying multiple pending transactions automatically

Each transaction includes:

- `id`: Unique identifier
- `name`: Descriptive name
- `date`: Date and time (stored in UTC)
- `value`: Monetary amount
- `status`: One of: `PENDING`, `PAID`, `REJECTED`

## 📌 API Endpoints — Quick Reference

### Get Transactions (with filters, pagination, sorting)
```http
GET /api/transactions?name=Rent&status=PENDING&page=0&size=10&sort=date,desc
```

###  Post Transactions
```http
POST /api/transactions
Content-Type: application/json

{
"name": "Rent",
"date": "2025-07-29T10:00:00-05:00",
"value": 1200.0,
"status": "PENDING"
}
```

### Update Transactions
```http
PUT /api/transactions/1
Content-Type: application/json

{
  "name": "Updated Rent",
  "date": "2025-08-01T00:00:00Z",
  "value": 1300.0,
  "status": "PENDING"
}
```

### Delete Transactions
```http
DELETE /api/transactions/1
```

### Make Payment
```http
POST /api/transactions/payment?paymentValue=1000.0
```
