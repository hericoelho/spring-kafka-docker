package com.example.springqueue.infrastructure.adapter.in.kafka;

import com.example.springqueue.application.ports.in.ReceiveTransactionUseCase;
import com.example.springqueue.domain.model.TransactionEvent;
import com.example.springqueue.domain.model.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RetryableTopic(
        attempts = "3",
        backOff = @BackOff(delay = 1000, multiplier = 2.0),
        dltStrategy = DltStrategy.FAIL_ON_ERROR,
        exclude = BusinessException.class  // Erro de negócio → DLQ direto
)
public class KafkaTransactionListener {

    private final ReceiveTransactionUseCase receiveTransactionUseCase;

    @KafkaListener(
            topics = "${app.kafka.topics.transaction-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(
            TransactionEvent transactionEvent,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ){
        log.info("Received transaction: id={}, topic={}, partition={}, offset={}",
                transactionEvent.id(), topic, partition, offset);

        try {
            boolean processed = receiveTransactionUseCase.receive(transactionEvent);
            if (processed) {
                log.info("Transaction processed successfully: id={}", transactionEvent.id());
            }
        }  catch (BusinessException e) {
            log.error("Business error processing transaction: id={}, msg={}", transactionEvent.id(), e.getMessage());
            throw e;
        }  catch (Exception e) {
            log.error("Generic error processing transaction: id={}", transactionEvent.id(), e);
            throw e;
        } finally {
            acknowledgment.acknowledge();
        }
    }

    @DltHandler
    public void onDltMessage(
            TransactionEvent transactionEvent,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.error("DLQ: Failed after retries: id={}, topic={}, partition={}, offset={}",
                transactionEvent.id(), topic, partition, offset);
        acknowledgment.acknowledge();
    }
}
