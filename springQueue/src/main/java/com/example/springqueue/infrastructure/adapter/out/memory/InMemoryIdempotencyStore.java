package com.example.springqueue.infrastructure.adapter.out.memory;

import com.example.springqueue.application.ports.out.IdempotencyPort;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryIdempotencyStore implements IdempotencyPort {
    private final Set<String> processedIds = ConcurrentHashMap.newKeySet();

    @Override
    public boolean tryAcquire(String transactionId) {
        return processedIds.add(transactionId);
    }
}
