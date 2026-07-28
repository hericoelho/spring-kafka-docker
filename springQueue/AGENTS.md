# Spring Microservice - AGENTS.md

## Architectural Pillars

### 1. Spring Boot 3+ Idiomatic Design
* Leverage Spring Boot native capabilities. Avoid raw thread pools or manual boilerplate.
* Use `Records` for DTOs and Kafka Events (Java 17 immutable data structures).
* Implement central exception handling using `@RestControllerAdvice` and RFC 7807 Problem Details.
* Structured logging using SLF4J with clear tracking context (MDC for correlation IDs if applicable).

### 2. Architecture: Hexagonal (Ports & Adapters)
* Keep the Domain/Core logic completely decoupled from framework-specific code.
* **Core/Domain:** Contains pure Java business rules, Domain Services, and Entities.
* **Ports (Interfaces):** Inbound (Use Cases/Input Boundary) and Outbound (Output Boundary for DB/Kafka).
* **Adapters (Infrastructure):** Spring Controllers, Kafka Listeners/Producers, Spring Data Repositories.
* *Rule:* Infrastructure can depend on the Domain, but the Domain must NEVER depend on Spring, Kafka, or Database drivers.

### 3. High-Scale Kafka Resiliency (FinTech Level)
* **Asynchronous Execution:** HTTP Endpoints must validate input (`jakarta.validation`), send to Kafka, and immediately return `202 Accepted` with a protocol ID. No thread-blocking waiting for processing.
* **Strict Idempotency:** The Consumer MUST check if a transaction ID has already been processed (using Redis or DB constraints) to prevent duplicate charges from network retries.
* **Fault Tolerance & DLQ:** Implement a non-blocking retry mechanism using `@RetryableTopic` with Exponential Backoff (e.g., 3 attempts, multiplier 2.0).
* **Dead Letter Queue (DLQ):** Unrecoverable or exhausted failures must safely route to a `-dlq` topic with an explicit `@DltHandler` for operational auditing.

---

## Project Status

### Milestones
| Milestone | Status | Scope |
|-----------|--------|-------|
| M1: Infra + Contracts | ✅ DONE | docker-compose, domain Records, project structure |
| M2: Inbound Adapter | ✅ DONE | Controller, UseCase, validation, RFC 7807, logging, actuator |
| M3: Outbound Adapter | ✅ DONE | Kafka Producer, config, topic via @Value |
| M4: Consumer + Retry/DLQ | ✅ DONE | @KafkaListener, idempotency, @RetryableTopic, @DltHandler |
| M5: Resiliência | ✅ DONE | Circuit Breaker (Resilience4j), distributed tracing (Micrometer + Zipkin), Kafka observation |

### Interview Checklist
- [x] Spring Boot 3+ auto-configuration
- [x] `@RestControllerAdvice` + RFC 7807 Problem Details
- [x] `jakarta.validation` for request validation
- [x] Records as DTOs (Java 17)
- [x] Structured logging (SLF4J + MDC correlation IDs)
- [x] Actuator endpoints (health, info, metrics)
- [x] Producer configuration (acks, retries, idempotence)
- [x] Consumer groups & partition assignment
- [x] Offset management (manual commit strategies)
- [x] Key serialization strategy (partitioning by key)
- [x] Topic design (partitions, replication factor)
- [x] Idempotent consumer (DB constraint or Redis check)
- [x] `@RetryableTopic` with exponential backoff
- [x] Dead Letter Queue (DLQ) with `@DltHandler`
- [x] Circuit Breaker (Resilience4j)
- [x] Distributed tracing (Micrometer + Zipkin/Jaeger)
- [x] Hexagonal / Ports & Adapters isolation
- [x] Domain layer free of framework dependencies
- [x] UseCase interfaces (inbound/outbound ports)
- [x] Adapter layer (controllers, listeners, repositories)

---

## Domain Definition

**Microservice:** PIX Transaction Queue

### Business Flow
```
Client → POST /api/v1/transactions → Controller (validate) → UseCase (enrich) → Kafka Producer → Topic
                                                                                                   ↓
                                                                                            Consumer (idempotent) → Process payment
```

### Domain Records
| Record | Fields | Purpose |
|--------|--------|---------|
| `TransactionEvent` | id, keyPix, amount(BigDecimal), payerDocument, timestamp, status | Domain model (Kafka payload) |
| `TransactionStatus` | RECEIVED, PROCESSING, COMPLETED, FAILED | State machine |
| `TransactionRequest` | keyPix, amount, payerDocument | API input DTO |
| `TransactionResponse` | protocolId | API output DTO (202 Accepted) |

### Key Design Decisions
- `BigDecimal` for monetary values (never `double` — floating point precision errors)
- `LocalDateTime` for timestamps
- Partial constructor on `TransactionEvent` (without id/status/timestamp) for mapper
- UseCase enriches event with UUID, timestamp, and initial status
- Returns `String` (protocolId) from UseCase to Controller
- Topic name externalized via `@Value("${app.kafka.topics.transaction-events}")`
- Sensitive data (keyPix, payerDocument) masked in logs via `SensitiveDataMask`

---

## Project Structure

```
com.example.springqueue
├── domain/model/
│   ├── TransactionEvent.java              # Record (domain)
│   └── TransactionStatus.java             # Enum
├── domain/model/exception/
│   └── BusinessException.java             # Domain exception (skip retry)
├── application/
│   ├── ports/in/
│   │   ├── ProcessTransactionUseCase.java # Interface (inbound)
│   │   └── ReceiveTransactionUseCase.java # Interface (inbound)
│   ├── ports/out/
│   │   ├── PublishTransactionPort.java    # Interface (outbound)
│   │   └── IdempotencyPort.java          # Interface (outbound)
│   └── usecases/
│       ├── ProcessTransactionUseCaseImpl.java
│       └── ReceiveTransactionUseCaseImpl.java
├── infrastructure/
│   ├── config/
│   │   └── TransactionConfig.java         # @Configuration (wires UseCase beans only)
│   ├── logging/
│   │   └── SensitiveDataMask.java         # Utility (mask PII in logs)
│   ├── adapter/
│   │   ├── in/kafka/
│   │   │   └── KafkaTransactionListener.java  # @KafkaListener + @RetryableTopic
│   │   ├── in/web/
│   │   │   ├── controller/
│   │   │   │   └── TransactionController.java
│   │   │   ├── dto/
│   │   │   │   ├── TransactionRequest.java
│   │   │   │   └── TransactionResponse.java
│   │   │   ├── exception/
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   └── filter/
│   │   │       └── CorrelationIdFilter.java
│   │   └── out/kafka/
│   │       └── KafkaTransactionPublisher.java
│   │   └── out/memory/
│   │       └── InMemoryIdempotencyStore.java
│   └── mapper/
│       └── TransactionEventMapper.java
```

### Dependencies (pom.xml)
- `spring-boot-starter-webmvc`
- `spring-boot-starter-kafka`
- `spring-boot-starter-json` (Jackson 3.x — primary JSON support)
- `com.fasterxml.jackson.core:jackson-databind` with `<scope>compile</scope>` (Jackson 2.x — required by Kafka client)
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `io.github.resilience4j:resilience4j-spring-boot3:2.2.0` (Circuit Breaker — version not managed by Spring Boot BOM)
- `spring-boot-starter-zipkin` (Distributed Tracing — managed by Spring Boot BOM)
- `lombok`
- `spring-boot-starter-kafka-test` (test)
- `spring-boot-starter-webmvc-test` (test)

---

## Conventions & Patterns

### Logging
- Use `@Slf4j` (Lombok) in all infrastructure classes
- Use `SensitiveDataMask.mask()` for PII data (keyPix, payerDocument)
- Log levels: `info` for flow, `warn` for validation errors, `error` for unexpected exceptions
- Correlation ID via `CorrelationIdFilter` + MDC (appears in all logs automatically)

### Validation
- `@NotBlank` for Strings (not `@NotEmpty` — rejects whitespace-only)
- `@NotNull` + `@DecimalMin` for BigDecimal
- `@Valid` on `@RequestBody` in Controller

### HTTP Responses
- POST returns `202 Accepted` with `TransactionResponse(protocolId)`
- Validation errors return RFC 7807 ProblemDetail (400) with field errors map
- Unexpected errors return RFC 7807 ProblemDetail (500)

### Kafka Producer
- Topic name from `@Value("${app.kafka.topics.transaction-events}")`
- Key: `transactionEvent.id()` (UUID String) — ensures partition consistency
- Value: `TransactionEvent` (JSON via JacksonJsonSerializer — auto-configured by Spring Boot)
- Producer config via `application.properties` (Spring Boot auto-config creates `KafkaTemplate`)
- Config: `acks=all`, `enable.idempotence=true`, `retries=3`

### Hexagonal Architecture
- Domain: NO Spring/Kafka imports
- Ports: Interfaces in `application/ports/`
- Adapters: `@Component` implementations in `infrastructure/adapter/`
- Config: `@Configuration` beans in `infrastructure/config/` (manual wiring, not component scan)
- KafkaTemplate is auto-configured via `application.properties` — no manual `@Bean` needed

---

## Next Steps (M5: Resiliência)

### What was implemented
1. ~~Circuit Breaker with Resilience4j~~ ✅ Done
2. ~~Distributed tracing (Micrometer + Zipkin/Jaeger)~~ ✅ Done
3. ~~Consumer groups & partition assignment configuration~~ ✅ Done
4. ~~Offset management (manual commit strategies)~~ ✅ Done
5. ~~Key serialization strategy (partitioning by key)~~ ✅ Done
6. ~~Topic design (partitions, replication factor)~~ ✅ Done

### Key review points
- Error classification: business errors (skip retry) vs infrastructure errors (trigger retry)
- Circuit Breaker state transitions and fallback behavior
- Trace propagation across Kafka producer → consumer
- Partition strategy for transaction ordering guarantees

---

## Review Focus (M2-M5)

| Milestone | Scope | Key Review Points |
|-----------|-------|-------------------|
| **M2: Inbound Adapter** | REST Controller, UseCase interface, validation | HTTP status codes, domain isolation, no anemia |
| **M3: Outbound Adapter** | Kafka Producer implementation | Thread-safety, serialization, async vs sync |
| **M4: Consumer + Retry/DLQ** | `@KafkaListener`, idempotency, `@RetryableTopic`, `@DltHandler` | Race conditions, double-spend prevention, error classification |
| **M5: Resiliency** | Circuit Breaker, tracing, topic design | Fallback behavior, trace propagation, partition strategy |

---

## Known Issues & Gotchas (Spring Boot 4.x)

### Jackson 2.x vs 3.x — Kafka Client Compatibility
**Symptom:** `ClassNotFoundException: com.fasterxml.jackson.databind.JavaType` at startup.
**Root cause:** Spring Boot 4.x migrated to Jackson 3.x (`tools.jackson.*`), but the Apache Kafka client (`kafka-clients`) still depends on Jackson 2.x (`com.fasterxml.jackson.*`). The `spring-boot-starter-json` BOM manages `com.fasterxml.jackson.core:jackson-databind` with **scope `test`** — so it's absent from the runtime classpath.

**Fix:** Add Jackson 2.x **explicitly with `<scope>compile</scope>`**:
```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <scope>compile</scope>
</dependency>
```

**Why it's tricky:** Simply adding the dependency without `<scope>compile</scope>` inherits the BOM's `test` scope — Maven's `dependencyManagement` scope defaults override the implicit `compile` scope. Verify with:
```bash
./mvnw dependency:tree | grep "com.fasterxml.jackson.core:jackson-databind"
# Must show: compile (not test)
```

### Docker Build Cache
**Symptom:** Dependency fix applied to `pom.xml` but error persists inside container.
**Root cause:** `COPY . .` in Dockerfile reuses cached layers. Maven `.m2/repository` inside the build stage has stale dependency resolution.

**Fix:** Always rebuild with `--no-cache` after changing dependencies:
```bash
podman compose -f docker-compose.yml build --no-cache spring-app
```

### Auto-Configuration vs Manual KafkaTemplate (Jackson 2.x vs 3.x)
**Symptom:** `SerializationException: Can't serialize data` with `InvalidDefinitionException: Java 8 date/time type LocalDateTime not supported by default`.
**Root cause:** Spring Boot 4.x auto-configures a `KafkaTemplate<String, Object>` using `spring.kafka.producer.value-serializer` from `application.properties`. If you also define a manual `@Bean KafkaTemplate<String, TransactionEvent>`, both beans exist. The auto-configured one (using deprecated Jackson 2.x `JsonSerializer`) wins at runtime.

**Fix:** Do NOT define a manual `KafkaTemplate` bean. Use Spring Boot auto-configuration exclusively:
1. Set `spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer` (Jackson 3.x)
2. Set `spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JacksonJsonDeserializer` (Jackson 3.x)
3. Let `KafkaAutoConfiguration` create the `KafkaTemplate` — zero boilerplate

**Why it's tricky:** The deprecated `JsonSerializer` (Jackson 2.x) and the current `JacksonJsonSerializer` (Jackson 3.x) are both on the classpath. The auto-configured producer config log shows `value.serializer = class ...JsonSerializer` — if you see this instead of `JacksonJsonSerializer`, the wrong serializer is active.

### Spring Boot 4.x Deprecated Properties
**Symptom:** IDE warnings like `Cannot resolve configuration property` or `Deprecated configuration property` in `application.properties`.
**Root cause:** Spring Boot 4.x renamed several properties. Old names still work at runtime but trigger IDE warnings and may stop working in future versions.

| Deprecated (won't resolve) | Correct (Spring Boot 4.x) | Since |
|---|---|---|
| `management.tracing.enabled` | `management.tracing.export.enabled` | 4.0.0 |
| `management.zipkin.tracing.endpoint` | `management.tracing.export.zipkin.endpoint` | 3.2.0 |
| `spring.kafka.producer.max-in-flight-requests-per-connection` | `spring.kafka.producer.properties.max.in.flight.requests.per.connection` | Native Kafka prop |

### Spring Kafka 4.x — Observation Disabled by Default
**Symptom:** Zipkin receives HTTP spans but no Kafka producer/consumer spans. Traces are fragmented (different traceIds for HTTP vs Kafka).
**Root cause:** Spring Kafka 4.x defaults `observation-enabled` to `false` for both producer and consumer to avoid performance overhead. Without observation, the `KafkaTemplate` does not set `traceparent`/`tracestate` headers on messages.

**Fix:** Enable observation explicitly in `application.properties`:
```properties
spring.kafka.template.observation-enabled=true    # Producer: creates PRODUCE span + sets trace headers
spring.kafka.listener.observation-enabled=true    # Consumer: creates CONSUME span + reads trace headers
```

**Why it's tricky:** `spring.kafka.producer.properties.observation.enabled=true` is a **Kafka native client property** (for internal metrics), NOT the Spring Kafka observation property. It does NOT enable tracing. The correct property is `spring.kafka.template.observation-enabled`.

---

## Testing Conventions

### Naming & Structure (JUnit 5 + `@Nested`)
Use `@DisplayName` with Given/When/Then hierarchy via `@Nested` classes:

```java
@DisplayName("Given a <Sut>")
class <Sut>Test {

    @Nested
    @DisplayName("When <scenario>")
    class When<Scenario> {

        @Test
        @DisplayName("given <condition>, then <expected behavior>")
        void should<ExpectedBehavior>() {
            // 1 assert per test
        }
    }
}
```

Rules:
- **1 assert per test** — one logical assertion (`assertEquals`, `assertTrue`, etc.)
- **Test file mirrors source package** exactly (e.g. `SensitiveDataMask` → `infrastructure/logging/SensitiveDataMaskTest`)
- **No `@SpringBootTest` for unit tests** — pure JUnit + Mockito when needed
- **English only** — names, descriptions, and messages

### Mutation Testing (PIT)
Mutation coverage with PIT (`org.pitest:pitest-maven`). Goal: >90% mutation coverage.

- **Run:** `./mvnw test org.pitest:pitest-maven:mutationCoverage`
- **Report:** `target/pit-reports/index.html` (visual HTML com detalhes de mutações sobreviventes)
- **Common survivor:** `ConditionalsBoundaryMutator` em `<=` / `<` — sempre testar valores exatos do boundary (ex: length == 4 para `<= 4`)
- **Exclusions:** `TransactionController` excluído do alvo de mutação (requer contexto Spring mock bean)
- **Dependencies no pom.xml:**
  ```xml
  <plugin>
      <groupId>org.pitest</groupId>
      <artifactId>pitest-maven</artifactId>
      <version>1.19.1</version>
      <configuration>
          <mutationThreshold>90</mutationThreshold>
      </configuration>
      <dependencies>
          <dependency>
              <groupId>org.pitest</groupId>
              <artifactId>pitest-junit5-plugin</artifactId>
              <version>1.2.2</version>
            </dependency>
        </dependencies>
    </plugin>
  ```

### Test Types per Layer

| Layer | Testing approach | Dependencies |
|-------|-----------------|--------------|
| **Domain** (model, utils) | Pure JUnit 5, no mocking | None |
| **Application** (usecases) | JUnit 5 + Mockito for ports | `IdempotencyPort`, `PublishTransactionPort` |
| **Infrastructure** (mappers) | Pure JUnit 5 | None |
| **Web** (controllers) | `@WebMvcTest` slice test + Mockito for usecases | `spring-boot-starter-webmvc-test` |
| **Infrastructure** (Kafka) | `@SpringBootTest` + `@EmbeddedKafka` | `spring-boot-starter-kafka-test` |

### Mockito Conventions
- Use `@Mock` + `@InjectMocks` (not `MockitoAnnotations.openMocks(this)` / not manual `new`)
- Use `BDDMockito.given()` over `when()` for readability
- Never mock types you don't own (`String`, `UUID`, `BigDecimal`)

---

## Agent Interaction

To start or resume a step, paste your code and ask:
1. *"Review this code against our AGENTS.md requirements. Is it Senior level?"*
2. *"Are there any edge cases, race conditions, or architecture leaks in this Hexagonal implementation?"*
3. *"How can I refactor this to better match idiomatic Spring Boot 3 standards?"*
