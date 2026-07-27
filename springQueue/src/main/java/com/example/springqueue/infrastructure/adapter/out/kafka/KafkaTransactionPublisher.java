package com.example.springqueue.infrastructure.adapter.out.kafka;

import com.example.springqueue.application.ports.out.PublishTransactionPort;
import com.example.springqueue.domain.model.TransactionEvent;
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
    public void publish(TransactionEvent transactionEvent) {
        kafkaTemplate.send(transactionTopic, transactionEvent.id(), transactionEvent);
        log.info("Sent to Kafka: topic={}, key={}", transactionTopic, transactionEvent.id());
    }
}
