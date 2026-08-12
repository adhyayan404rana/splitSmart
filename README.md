# SplitSmart — Intelligent Group Expense Reconciliation

[![CI/CD Pipeline](https://github.com/splitsmart/splitsmart/actions/workflows/ci.yml/badge.svg)](.github/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java 23](https://img.shields.io/badge/Java-23-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot 3.4](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React 18](https://img.shields.io/badge/React-18-61dafb.svg)](https://react.dev/)

> **Product Vision**: Create a frictionless, zero-entry expense sharing experience where users spend no time manually logging receipts, while maintaining absolute trust and transparency in their shared financial obligations.

---

## Architecture Blueprint & Philosophy

SplitSmart adheres to the core principle: **Deterministic Financials, Asynchronous AI**.
Financial ledgers cannot afford probabilistic errors. AI is strictly relegated to intent extraction at the ingestion boundary, while all mutations are processed by an immutable, event-sourced state machine.

```
[ External Webhooks: Telegram / WhatsApp ]
                    │ (JSON Payloads)
                    ▼
  [ API Gateway & Rate Limiter ] (Redis Token Bucket)
                    │
                    ▼
    [ Message Broker ] (RabbitMQ Queue)
                    │ (Async processing queue)
                    ▼
     [ NLP Ingestion Worker ]
   ├── Fast Path: Aho-Corasick / Regex pre-filter
   ├── ML Path: Quantized ONNX NER Model (Payer, Amount, Entities)
   └── Fallback Path: Structured Output LLM (Strict JSON Schema)
                    │
                    ▼
 [ Idempotency & Concurrency Layer ] (Redis Distributed Locks)
                    │
                    ▼
 [ Ledger Command Service ] (Event Sourcing Write Node)
   └── Append to Immutable Event Store (PostgreSQL)
                    │
                    ▼
 [ Materialized View Updater ] (Read Node)
   └── Computes Current Ledger State
                    │
                    ▼
   [ Settlement Engine ] (DSA Core)
   ├── Fast: Max Flow / Min Cut Simplification
   └── Exact: DP with Bitmasking (Subset Sum solver for N < 15)
```

---

## Bounded Contexts (Monolith Directory Layout)

```
splitSmart/
├── backend/                  # Java 23 / Spring Boot 3.4 Modular Monolith
│   ├── src/main/java/com/splitsmart/
│   │   ├── api/              # Gateway, Rate Limiting & Webhooks
│   │   ├── auth/             # JWT, RBAC & PostgreSQL Row-Level Security
│   │   ├── ingestion/        # 3-Tier NLP Pipeline & RabbitMQ Workers
│   │   ├── ledger/           # Append-Only Event Store & Materialized Views
│   │   ├── settlement/       # DP-Bitmask & Greedy Graph Algorithms
│   │   └── notification/     # FCM Push Notifications & WebSockets/SSE
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/     # Flyway Migration Baseline (V1__init_schema.sql)
├── frontend/                 # React 18 + Vite + TypeScript Application
├── k8s/                      # Kubernetes Deployments, Services, ConfigMaps & HPA
├── .github/workflows/        # Automated CI/CD Pipeline
└── docker-compose.yml        # Local Infrastructure Orchestrator (Postgres, Redis, RabbitMQ)
```

---

## Local Developer Environment Setup

### 1. Prerequisites
- **Java**: Java 23 JDK
- **Node.js**: Node.js 20+ & `npm`
- **Docker**: Docker Desktop (for containerized PostgreSQL, Redis, and RabbitMQ)

### 2. Start Infrastructure Services
Launch local database, caching, and message broker containers:
```bash
docker-compose up -d
```
Service endpoints:
- **PostgreSQL**: `localhost:5432` (`splitsmart` DB)
- **Redis**: `localhost:6379`
- **RabbitMQ AMQP**: `localhost:5672`
- **RabbitMQ Web Console**: `http://localhost:15672` (Credentials: `splitsmart_guest` / `splitsmart_guest`)

### 3. Run Backend (Spring Boot)
Navigate to `backend/` and execute using the Maven Wrapper:
```bash
cd backend
./mvnw spring-boot:run
```
- **Backend API**: `http://localhost:8080`
- **Health Actuator**: `http://localhost:8080/actuator/health`

### 4. Run Frontend (React + Vite)
In a separate terminal, navigate to `frontend/`:
```bash
cd frontend
npm install
npm run dev
```
- **Frontend App**: `http://localhost:3000`

---

## Verified Non-Functional Requirements & Security
- **Fowler Money Pattern**: All monetary values are strictly represented as integers (paise/cents). Floating point arithmetic (`float64`) is banned system-wide.
- **Row-Level Security (RLS)**: Enforced in PostgreSQL queries (`WHERE group_id IN (SELECT group_id FROM user_groups WHERE user_id = current_user_id)`).
- **Optimistic Concurrency Control (OCC)**: Version-tagged expense drafts to prevent lost updates during concurrent edits.
- **Idempotency**: Webhook idempotency keys generated via `SHA256(ChatID + Timestamp + PayerID)` cached in Redis for 48 hours.
- **Dynamic UPI Intent & QR Generation**: Generates spec-compliant `upi://pay` payment intent strings and Base64 PNG QR code URIs.
- **Architecture Decision Record**: Read [ADR-001](ADR_001_EVENT_SOURCED_LEDGER_AND_SETTLEMENT.md) for architectural trade-off decisions.

---

## Milestone Roadmap

- [x] **Milestone 1 — Project Foundation & Developer Environment**
- [x] **Milestone 2 — Authentication & Authorization Module**
- [x] **Milestone 3 — API Gateway, Rate Limiting & Webhook Ingress**
- [x] **Milestone 4 — AI Ingestion Pipeline (NLP Worker)**
- [x] **Milestone 5 — Event-Sourced Ledger & Draft Module**
- [x] **Milestone 6 — Notification & Draft Approval Workflow**
- [x] **Milestone 7 — Settlement Engine (Graph Algorithms Core)**
- [x] **Milestone 8 — Payment Execution & UPI Integration**
- [x] **Milestone 9 — Frontend, UX Polish & WCAG Compliance**
- [x] **Milestone 10 — Observability, Security & Production Deployment**
