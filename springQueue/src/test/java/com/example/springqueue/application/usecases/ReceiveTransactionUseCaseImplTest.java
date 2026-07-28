package com.example.springqueue.application.usecases;

import com.example.springqueue.application.ports.out.IdempotencyPort;
import com.example.springqueue.domain.model.TransactionEvent;
import com.example.springqueue.domain.model.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("Given a ReceiveTransactionUseCaseImpl")
class ReceiveTransactionUseCaseImplTest {

    @Mock
    private IdempotencyPort idempotencyPort;

    @InjectMocks
    private ReceiveTransactionUseCaseImpl useCase;

    @Nested
    @DisplayName("When receive with a new transaction")
    class WhenNewTransaction {

        @Test
        @DisplayName("given idempotency returns true, then should return true")
        void shouldReturnTrueWhenNew() {
            var event = createEvent(UUID.randomUUID().toString());
            given(idempotencyPort.tryAcquire(event.id())).willReturn(true);

            var result = useCase.receive(event);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("When receive with a duplicate transaction")
    class WhenDuplicateTransaction {

        @Test
        @DisplayName("given idempotency returns false, then should return false")
        void shouldReturnFalseWhenDuplicate() {
            var event = createEvent("dup-id");
            given(idempotencyPort.tryAcquire(event.id())).willReturn(false);

            var result = useCase.receive(event);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("When receive with transaction ID containing 'fail'")
    class WhenFailTransaction {

        @Test
        @DisplayName("given idempotency returns true, then should throw BusinessException")
        void shouldThrowBusinessException() {
            var event = createEvent("fail-123");
            given(idempotencyPort.tryAcquire(event.id())).willReturn(true);

            assertThrows(BusinessException.class, () -> useCase.receive(event));
        }
    }

    private TransactionEvent createEvent(String id) {
        return new TransactionEvent(
                id,
                "key-pix-abc",
                new BigDecimal("50.00"),
                "doc-123",
                LocalDateTime.now(),
                null
        );
    }
}
