# springQueue

Microserviço de processamento assíncrono de transações PIX utilizando fila Kafka com arquitetura hexagonal (Ports & Adapters).

**Fluxo:** `POST /api/v1/transactions` → validação → Kafka Producer → Consumer idempotente → processamento

---

## Stack

| Tecnologia | Versão |
|---|---|
| Java | 17 (Eclipse Temurin) |
| Spring Boot | 4.1.0 |
| Spring Kafka | gerenciado pelo BOM 4.1.0 |
| Spring WebMVC | gerenciado pelo BOM 4.1.0 |
| Resilience4j (Circuit Breaker) | 2.2.0 |
| Lombok | 1.18.46 |
| Jackson (Kafka compat) | 2.x (`compile` scope explícito) |
| Micrometer Tracing + Zipkin | gerenciado pelo BOM 4.1.0 |
| PIT Mutation Testing | 1.19.1 |
| Maven | 3.8.8+ |

---

## Arquitetura Hexagonal

```
┌─────────────────────────────────────────────────┐
│                  INFRASTRUCTURE                  │
│  ┌──────────┐  ┌──────────────┐  ┌───────────┐  │
│  │ Controller│  │ KafkaListener│  │KafkaProd. │  │
│  └────┬─────┘  └──────┬───────┘  └─────┬─────┘  │
│       │               │                │         │
├───────┴───────────────┴────────────────┴─────────┤
│                   APPLICATION                     │
│       ┌──────────────────────────────┐           │
│       │      UseCases (regras)       │           │
│       └──────────┬─────────┬─────────┘           │
│                  │         │                      │
│       ┌──────────┴┐  ┌────┴──────────┐           │
│       │ Inbound   │  │ Outbound      │           │
│       │ Ports     │  │ Ports         │           │
│       └───────────┘  └───────────────┘           │
├──────────────────────────────────────────────────┤
│                    DOMAIN                         │
│          Records, Enums, Exceptions               │
│          (zero dependência externa)               │
└──────────────────────────────────────────────────┘
```

A camada **Domain** não possui dependência alguma de Spring, Kafka ou banco de dados.

---

## Pré-requisitos

- Docker + Docker Compose
- Java 17+
- Maven (ou `./mvnw` incluso no projeto)

---

## Como Executar

### 1. Subir o Kafka (KRaft mode)

```bash
docker compose -f kafka/docker-compose.yml up -d
```

### 2. Executar a aplicação

**Local (Spring Boot):**

```bash
./mvnw spring-boot:run
```

**Container Docker:**

```bash
docker compose up --build
```

A aplicação se conecta automaticamente ao Kafka no endereço configurado via variável de ambiente `SPRING_KAFKA_BOOTSTRAP_SERVERS`.

---

## Testes

### Unitários

```bash
./mvnw test
```

### Mutação (PIT)

```bash
./mvnw test org.pitest:pitest-maven:mutationCoverage
```

Relatório HTML disponível em `target/pit-reports/index.html`. Meta: >90% de mutation coverage.

---

## Configurações do Kafka

### Producer

| Propriedade | Valor | Motivo |
|---|---|---|
| `acks` | `all` | Confirmação de todas as réplicas (FinTech) |
| `enable.idempotence` | `true` | Prevenção de duplicatas no broker |
| `retries` | `3` | Resiliência a falhas transitórias |
| `max.in.flight.requests.per.connection` | `5` | Performance com idempotência habilitada |
| `linger.ms` | `10` | Pequeno batch para throughput |
| `value-serializer` | `JacksonJsonSerializer` | Jackson 3.x (Spring Kafka moderno) |

### Consumer

| Propriedade | Valor |
|---|---|
| `ack-mode` | `MANUAL_IMMEDIATE` |
| `auto-offset-reset` | `earliest` |
| `group-id` | `transaction-group` |
| `partition.assignment.strategy` | `CooperativeStickyAssignor` |

### Retry & DLQ

Configurado via `@RetryableTopic` no listener:

- **3 tentativas** com backoff exponencial: 1s → 2s → 4s
- `BusinessException` (erro de negócio) → **DLQ direto**, sem retry
- Exceções não classificadas passam pelas 3 tentativas e depois vão para a DLQ
- Tópico DLQ: `transaction-events-dlt` com handler `@DltHandler`

### Observação (Tracing)

```properties
spring.kafka.template.observation-enabled=true
spring.kafka.listener.observation-enabled=true
```

Propaga headers `traceparent`/`tracestate` entre producer e consumer para tracing distribuído com Micrometer + Zipkin.

### Tópico

- **Nome:** `transaction-events` (externalizado via `app.kafka.topics.transaction-events`)
- **Partições:** 3
- **Réplicas:** 1
- Criado automaticamente via `NewTopic` bean em `TopicConfig.java`

---

## API

### `POST /api/v1/transactions`

Envia uma transação para processamento assíncrono.

**Request:**
```json
{
  "keyPix": "chave-pix-123",
  "amount": 150.00,
  "payerDocument": "111.222.333-44"
}
```

**Response** `202 Accepted`:
```json
{
  "protocolId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Validações:**
- `keyPix`: obrigatório, não pode ser blank
- `amount`: obrigatório, mínimo 0.01
- `payerDocument`: obrigatório, não pode ser blank

Erros de validação retornam `400 Bad Request` com RFC 7807 ProblemDetail.

### Actuator

| Endpoint | Descrição |
|---|---|
| `GET /actuator/health` | Health check com detalhes |
| `GET /actuator/info` | Informações do aplicativo |
| `GET /actuator/metrics` | Métricas (Kafka, JVM, etc.) |

### Kafka Metrics (Micrometer)

Métricas do cliente Kafka (consumer lag, throughput, taxas de erro) expostas via Micrometer:

```bash
# Listar todas as métricas do Kafka
curl -s http://localhost:8080/actuator/metrics | jq '.names | map(select(startswith("kafka")))'

# Ver lag do consumidor por partição
curl -s http://localhost:8080/actuator/metrics/kafka.consumer.fetch.manager.records.lag | jq .

# Ver taxa de envio do producer
curl -s http://localhost:8080/actuator/metrics/kafka.producer.record.send.rate | jq .
```

As métricas são registradas automaticamente via `MicrometerConsumerListener` e `MicrometerProducerListener` no `KafkaMetricsConfig.java`.

---

## Estrutura do Projeto

```
src/main/java/com/example/springqueue/
├── domain/
│   └── model/                          # Records imutáveis
│       ├── TransactionEvent.java
│       ├── TransactionStatus.java
│       └── exception/
│           └── BusinessException.java
├── application/
│   ├── ports/
│   │   ├── in/                         # Inbound ports (use cases)
│   │   │   ├── ProcessTransactionUseCase.java
│   │   │   └── ReceiveTransactionUseCase.java
│   │   └── out/                        # Outbound ports
│   │       ├── IdempotencyPort.java
│   │       └── PublishTransactionPort.java
│   └── usecases/                       # Regras de negócio
│       ├── ProcessTransactionUseCaseImpl.java
│       └── ReceiveTransactionUseCaseImpl.java
└── infrastructure/
    ├── adapter/
    │   ├── in/web/                     # REST Adapter
    │   │   ├── controller/TransactionController.java
    │   │   ├── dto/TransactionRequest.java
    │   │   ├── dto/TransactionResponse.java
    │   │   ├── exception/GlobalExceptionHandler.java
    │   │   └── filter/CorrelationIdFilter.java
    │   ├── in/kafka/                   # Kafka Listener Adapter
    │   │   └── KafkaTransactionListener.java
    │   ├── out/kafka/                  # Kafka Producer Adapter
    │   │   └── KafkaTransactionPublisher.java
    │   └── out/memory/                 # Idempotency Store (em memória)
    │       └── InMemoryIdempotencyStore.java
    ├── config/                         # Beans de configuração
    │   ├── KafkaMetricsConfig.java     # Kafka metrics via Micrometer
    │   ├── TopicConfig.java
    │   └── TransactionConfig.java
    ├── logging/
    │   └── SensitiveDataMask.java      # Mascaramento de PII em logs
    └── mapper/
        └── TransactionEventMapper.java
```
