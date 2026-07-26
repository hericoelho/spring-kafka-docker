package com.example.springqueue.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionEvent(
        String id,
        String keyPix,
        BigDecimal amount,
        String payerDocument,
        LocalDateTime timestamp,
        TransactionStatus status
) {
    public TransactionEvent(String keyPix, BigDecimal amount, String payerDocument){
        this(null, keyPix, amount, payerDocument, null, null);
    }
}
