package com.example.springqueue.application.ports.out;

public interface IdempotencyPort {
    boolean tryAcquire(String transactionId);
}
