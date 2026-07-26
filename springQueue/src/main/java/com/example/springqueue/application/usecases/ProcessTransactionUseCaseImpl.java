package com.example.springqueue.application.usecases;

import com.example.springqueue.application.ports.in.ProcessTransactionUseCase;
import com.example.springqueue.application.ports.out.PublishTransactionPort;
import com.example.springqueue.domain.model.TransactionEvent;
import com.example.springqueue.domain.model.TransactionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor  // ← Lombok gera o construtor com args
public class ProcessTransactionUseCaseImpl implements ProcessTransactionUseCase {

    private final PublishTransactionPort publishTransactionPort;

    @Override
    public String process(TransactionEvent transactionEvent) {
        String id = UUID.randomUUID().toString();
        LocalDateTime timestamp = LocalDateTime.now();
        TransactionStatus status = TransactionStatus.RECEIVED;
        TransactionEvent updatedTransactionEvent = new TransactionEvent(
                id,
                transactionEvent.keyPix(),
                transactionEvent.amount(),
                transactionEvent.payerDocument(),
                timestamp,
                status
        );

        log.info("Publishing transaction: id={}, status={}",
                updatedTransactionEvent.id(), updatedTransactionEvent.status());
        publishTransactionPort.publish(updatedTransactionEvent);

        return id;
    }
}
