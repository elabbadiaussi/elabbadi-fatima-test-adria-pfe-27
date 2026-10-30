# Partie B — Conception et Architecture

## 1. Diagramme de classes

```mermaid
classDiagram
    direction LR

    class AccountStatus {
        <<enumeration>>
        ACTIVE
        BLOCKED
    }

    class Account {
        +UUID id
        +String customerReference
        +AccountStatus status
        +String currency
        +double balance
    }

    class Transaction {
        +UUID id
        +UUID accountId
        +BigDecimal amount
        +String currency
        +String merchantCategory
        +String country
        +Instant createdAt
    }

    class TransferRequest {
        +UUID fromAccountId
        +UUID toAccountId
        +BigDecimal amount
        +String currency
    }

    class TransactionCreatedEvent {
        +UUID eventId
        +String eventType
        +UUID transactionId
        +UUID accountId
        +BigDecimal amount
        +String currency
        +Instant occurredAt
    }

    class AccountService {
        +findAll() List~Account~
        +findById(UUID) Account
        +create(Account) Account
        +updateStatus(UUID, AccountStatus) Account
        +debit(UUID, double) Account
        +credit(UUID, double) Account
    }

    class TransactionService {
        +findAll() List~Transaction~
        +findByAccount(UUID) List~Transaction~
        +create(Transaction) Transaction
        +transfer(TransferRequest) List~Transaction~
    }

    class AccountRestClient {
        <<FeignClient>>
        +getAccount(UUID) Account
        +debit(UUID, double) Account
        +credit(UUID, double) Account
    }

    class TransactionEventPublisher {
        +publish(TransactionCreatedEvent)
    }

    class TransactionEventConsumer {
        +onTransactionCreated(TransactionCreatedEvent)
    }

    Account --> AccountStatus
    Transaction ..> Account : accountId
    AccountService --> Account
    TransactionService --> Transaction
    TransactionService --> AccountRestClient : OpenFeign + Resilience4J
    TransactionService --> TransactionEventPublisher
    TransactionEventPublisher ..> TransactionCreatedEvent
    TransactionEventConsumer ..> TransactionCreatedEvent
    TransactionService ..> TransferRequest
```

## 2. Schéma d'architecture

```mermaid
flowchart LR
    subgraph Clients
        WEB["Angular Web App"]
        TG["Telegram / Discord"]
        REST["Client REST"]
    end

    KC["Keycloak<br/>(OAuth2 / JWT)"]

    subgraph SpringCloud["Spring Cloud"]
        GW["gateway-service<br/>:8888"]
        EU["discovery-service<br/>Eureka :8761"]
        CFG["config-service<br/>:9999"]
    end

    subgraph Metier["Microservices métier"]
        ACC["account-service :8081<br/>REST | GraphQL | MCP"]
        TRX["transaction-service :8082<br/>REST | GraphQL | MCP"]
    end

    BOT["bank-agent<br/>Spring AI + MCP Client"]
    LLM["LLM<br/>(OpenAI / Ollama)"]

    subgraph Async["Kafka"]
        TOPIC[["bank.transactions.created"]]
        CONS["audit-service<br/>(consumer)"]
    end

    ML["Python ML<br/>Détection de fraude"]
    GIT["config-repo<br/>bank.transaction.max-amount=1000"]

    WEB --> GW
    REST --> GW
    TG --> BOT
    WEB -. login .-> KC
    GW -. JWT .-> KC
    GW --> ACC
    GW --> TRX
    GW --> BOT
    TRX -- "OpenFeign + Resilience4J" --> ACC
    TRX -- publish --> TOPIC
    TOPIC --> CONS
    BOT -- MCP --> ACC
    BOT -- MCP --> TRX
    BOT --> LLM
    CFG --> GIT
    ACC -. register / config .-> EU
    TRX -. register / config .-> EU
    GW -. discover .-> EU
    ACC -.-> CFG
    TRX -.-> CFG
```

### Couches de chaque microservice métier

```
domain/        -> entités JPA
repository/    -> Spring Data JPA
service/       -> logique métier (règles)
web/rest/      -> @RestController   ┐
web/graphql/   -> @QueryMapping     ├─> appellent le même service
mcp/           -> @Tool (MCP)       ┘
```