# Inox & Outbox Patterns

**Implement Reliable Event-Driven Communication using Outbox & Inbox Patterns**
(in the `1-3-inbox-outbox` module)

## 🧠 **Task Overview**
**Outbox & Inbox Patterns** ensure reliable and idempotent event delivery in distributed systems.
They prevent message loss between database commits and message broker publishing (Outbox)
and guarantee that each event is processed exactly once (Inbox).

## 🎯 **Task Goal**
You are working on two microservices — **Order Service** and **Post-Order Service** — that communicate asynchronously using **Kafka**.

Your goal is to:
* Implement the **Outbox Pattern** in `order-service` to ensure atomicity between DB writes and event publishing.
* Implement the **Inbox Pattern** in `post-order-service` to ensure idempotent event consumption and processing.

**In short:**
Resolve all `TODOs` in:
* `order-service/*`
* `post-order-service/*`

**Message Flow**

```
OrderService  →  Outbox Table  →  Kafka  →  Inbox Table  →  PostOrderService
```

### 🚨 **Current Problem**
* Events can be lost if the service crashes after DB commit but before publishing to Kafka.
* Duplicate Kafka deliveries cause repeated business actions.
* No retry or audit visibility for event flow.

### 💡 **Hints**
<details>
<summary>Hint 1: Transactional Save</summary>
Use `@Transactional` in `OrderService#create(...)` to save both `Order` and `OutboxEvent` atomically.
</details>

<details>
<summary>Hint 2: Publishing</summary>
Poll Outbox events with status NEW or FAILED, publish via `KafkaTemplate`, and update their status to SENT.
</details>

<details>
<summary>Hint 3: Idempotent Consumer</summary>
Store each received Kafka message in `InboxEvent`.  
If already exists → skip. Otherwise process and mark `PROCESSED`.
</details>

<details>
<summary>Hint 4: Retry Logic</summary>
`InboxPoller` and `OutboxPoller` should retry FAILED messages periodically.
</details>

### 🧰 **Environment**
* **Kafka** + **Zookeeper** + **Postgres** (via Docker Compose)
* Each service has its own DB and schema (create automatically) (have example schema in resourses)

### ✅ **Success Criteria**
* No messages lost between DB and Kafka
* Duplicate events do not affect consumer logic
* `orders_count == inbox_count` in E2E tests
* logs show retries, fails, and delivery confirmation

```
Order Service (8080)
 ├── POST /api/orders → saves order + outbox event (1 transaction)
 └── OutboxPoller → publishes NEW/FAILED events to Kafka
 
Post-Order Service (8081)
 ├── Kafka listener → stores Inbox entry, processes message once
 └── InboxPoller → retries FAILED
```

### 📚 **Learning Resources**
* [Transactional Outbox pattern – microservices.io](https://microservices.io/patterns/data/transactional-outbox.html)
* [Outbox Pattern for Microservices Architectures](https://medium.com/design-microservices-architecture-with-patterns/outbox-pattern-for-microservices-architectures-1b8648dfaa27)
* [Inbox and outbox pattern](https://en.wikipedia.org/wiki/Inbox_and_outbox_pattern)
* [Outbox, Inbox patterns and delivery guarantees explained](https://event-driven.io/en/outbox_inbox_patterns_and_delivery_guarantees_explained/)