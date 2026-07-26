package com.example.springqueue.application.ports.in;

import com.example.springqueue.domain.model.TransactionEvent;

public interface ProcessTransactionUseCase {
    String process(TransactionEvent transactionEvent);
}
