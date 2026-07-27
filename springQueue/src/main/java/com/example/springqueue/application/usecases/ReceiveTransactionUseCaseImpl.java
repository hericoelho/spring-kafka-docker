package com.example.springqueue.application.usecases;

import com.example.springqueue.application.ports.in.ReceiveTransactionUseCase;
import com.example.springqueue.application.ports.out.IdempotencyPort;
import com.example.springqueue.domain.model.TransactionEvent;
import com.example.springqueue.domain.model.TransactionStatus;
import com.example.springqueue.domain.model.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ReceiveTransactionUseCaseImpl implements ReceiveTransactionUseCase {

    private final IdempotencyPort idempotencyPort;

    @Override
    public boolean receive(TransactionEvent transactionEvent) {
        if (!idempotencyPort.tryAcquire(transactionEvent.id())) {
            log.warn("Duplicate transaction ignored: id={}", transactionEvent.id());
            return false;
        }
        process(transactionEvent);
        return true;
    }

    private void process(TransactionEvent event) {
        if (event.id().contains("fail")) {
            throw new BusinessException("Simulated business error for: " + event.id());
        }
        TransactionEvent processed = new TransactionEvent(
                event.id(),
                event.keyPix(),
                event.amount(),
                event.payerDocument(),
                event.timestamp(),
                TransactionStatus.COMPLETED
        );
        log.info("Transaction processed: id={}, status={}", processed.id(), processed.status());
    }
}
