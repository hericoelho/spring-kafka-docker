package com.example.springqueue.infrastructure.adapter.out.kafka;

import com.example.springqueue.application.ports.out.PublishTransactionPort;
import com.example.springqueue.domain.model.TransactionEvent;
import com.example.springqueue.domain.model.exception.BusinessException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTransactionPublisher implements PublishTransactionPort {
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Value("${app.kafka.topics.transaction-events}")
    private String transactionTopic;

    @Override
    @CircuitBreaker(name = "kafkaPublisher", fallbackMethod = "publishFallback")
    public void publish(TransactionEvent transactionEvent) {
        kafkaTemplate.send(transactionTopic, transactionEvent.id(), transactionEvent);
        log.info("Sent to Kafka: topic={}, key={}", transactionTopic, transactionEvent.id());
    }

    public void publishFallback(TransactionEvent transactionEvent, Exception ex) {
        log.error("Circuit breaker fallback: unable to publish transaction id={}. Error: {}",
                transactionEvent.id(), ex.getMessage());
        throw new BusinessException("Kafka unavailable: " + ex.getMessage());
    }
}
