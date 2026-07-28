# spring-kafka-docker

Microserviço de processamento assíncrono de transações PIX utilizando fila Kafka com arquitetura hexagonal (Ports & Adapters).

**Fluxo:** `POST /api/v1/transactions` → validação → Kafka Producer → Consumer idempotente → processamento

---

## 📁 Estrutura do Repositório

```
spring-kafka/
├── kafka/                    # Infraestrutura: Kafka KRaft + AKHQ + Zipkin
│   ├── docker-compose.yml    # Orquestração dos serviços de infra
│   └── AGENTS.md             # Diretrizes da infra
├── springQueue/              # Spring Boot Application (ver README próprio)
│   ├── docker-compose.yml    # Orquestração do spring-app
│   ├── Dockerfile            # Build da imagem Spring Boot
│   ├── AGENTS.md             # Diretrizes do microsserviço
│   └── README.md             # Documentação detalhada do microsserviço
├── makefile                  # Atalhos para up/down do projeto completo
├── AGENTS.md                 # Diretrizes gerais de engenharia
└── README.md                 # Este arquivo
```

---

## 🧰 Stack & Versões

| Tecnologia | Versão |
|---|---|
| Java | 17 (Eclipse Temurin) |
| Spring Boot | 4.1.0 |
| Apache Kafka | 4.1.1 (KRaft mode — sem Zookeeper) |
| AKHQ (Kafka UI) | 0.27.0 |
| Zipkin (Distributed Tracing) | 3 (openzipkin) |
| Maven | 3.8.8+ |
| Resilience4j (Circuit Breaker) | 2.2.0 |
| Lombok | 1.18.46 |
| PIT Mutation Testing | 1.19.1 |
| Docker / Podman | Engine compatível com Compose |

---

## ✅ Pré-requisitos

- **Docker** ou **Podman** com suporte a Compose
- **Java 17+** (para execução local)
- **Maven 3.8.8+** (ou usar `./mvnw` incluso)
- **Git**

---

## 🚀 Como Executar

### Via Makefile (atalho)

```bash
# Sobe toda a infraestrutura (Kafka + AKHQ + Zipkin) e o Spring App
make exec

# Derruba tudo
make down
```

O Makefile detecta automaticamente entre `podman compose` e `docker compose`.

### Manualmente (passo a passo)

#### 1. Subir infraestrutura (Kafka + AKHQ + Zipkin)

```bash
docker compose -f kafka/docker-compose.yml up -d
```

#### 2. Executar o Spring App

**Opção A — Local (Spring Boot):**

```bash
cd springQueue && ./mvnw spring-boot:run
```

**Opção B — Container Docker:**

```bash
cd springQueue && docker compose up --build
```

> A aplicação conecta-se automaticamente ao Kafka via variável `SPRING_KAFKA_BOOTSTRAP_SERVERS`.

---

## 🔗 URLs

| Serviço | URL |
|---|---|
| API REST | `http://localhost:8080/api/v1/transactions` |
| AKHQ (Kafka UI) | `http://localhost:8081` |
| Zipkin (Tracing) | `http://localhost:9411` |
| Health Check | `http://localhost:8080/actuator/health` |

---

## 🏗️ Arquitetura (Alto Nível)

```
┌──────────┐     ┌────────────┐     ┌──────────────┐     ┌───────────┐
│  Client  │────▶│ Controller │────▶│ KafkaProducer│────▶│   Kafka   │
│ (HTTP)   │     │ (valida)   │     │ (assíncrono) │     │   Topic   │
└──────────┘     └────────────┘     └──────────────┘     └─────┬─────┘
                                                                │
                                                                ▼
                                                         ┌──────────────┐
                                                         │  Consumer    │
                                                         │ (idempotente)│
                                                         └──────┬───────┘
                                                                │
                                                                ▼
                                                         ┌──────────────┐
                                                         │   Process    │
                                                         │   Payment    │
                                                         └──────────────┘
```

- **REST**: Valida entrada com `jakarta.validation` e retorna `202 Accepted` com protocolId
- **Producer**: Publica evento no Kafka com `acks=all`, `enable.idempotence=true`
- **Consumer**: Processa com idempotência, retry com backoff exponencial e DLQ
- **Observability**: Tracing distribuído com Micrometer + Zipkin, logs com correlation ID

---

## 📚 Detalhes do Microsserviço

Para documentação completa do Spring Boot (stack, API, configurações Kafka, retry/DLQ, arquitetura hexagonal, testes), consulte:

➡️ **[springQueue/README.md](springQueue/README.md)**

---

## 📐 Convenções do Projeto

| Prática | Adotado |
|---|---|
| **Trunk-Based Development** | Commits diretamente na `main` |
| **Conventional Commits** | `feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:` |
| **Arquitetura** | Hexagonal (Ports & Adapters) — Domain sem dependência externa |
| **Idempotência** | `ConcurrentHashMap.newKeySet()` (dev) / Redis SETNX (produção) |
| **Resiliência** | `@RetryableTopic` com backoff exponencial + `@DltHandler` + Circuit Breaker (Resilience4j) |
| **Observabilidade** | Micrometer Tracing + Zipkin + MDC correlation ID |
| **Idioma** | Código e commits em inglês; documentação em português |
