package com.example.springqueue.infrastructure.adapter.out.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Given an InMemoryIdempotencyStore")
class InMemoryIdempotencyStoreTest {

    @Nested
    @DisplayName("When call tryAcquire with a new transaction ID")
    class WhenNewTransactionId {

        @Test
        @DisplayName("given unique ID, then should return true")
        void shouldReturnTrueForUniqueId() {
            var store = new InMemoryIdempotencyStore();

            var result = store.tryAcquire("transaction-123");

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("When call tryAcquire with a previously processed ID")
    class WhenDuplicateTransactionId {

        @Test
        @DisplayName("given duplicate ID, then should return false")
        void shouldReturnFalseForDuplicateId() {
            var store = new InMemoryIdempotencyStore();
            store.tryAcquire("transaction-123");

            var result = store.tryAcquire("transaction-123");

            assertFalse(result);
        }
    }
}
