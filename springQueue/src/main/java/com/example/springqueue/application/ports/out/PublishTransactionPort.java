package com.example.springqueue.application.ports.out;

import com.example.springqueue.domain.model.TransactionEvent;

public interface PublishTransactionPort {
    void publish(TransactionEvent transactionEvent);
}