package com.example.springqueue.application.usecases;

import com.example.springqueue.application.ports.out.PublishTransactionPort;
import com.example.springqueue.domain.model.TransactionEvent;
import com.example.springqueue.domain.model.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Given a ProcessTransactionUseCaseImpl")
class ProcessTransactionUseCaseImplTest {

    @Mock
    private PublishTransactionPort publishTransactionPort;

    @InjectMocks
    private ProcessTransactionUseCaseImpl useCase;

    @Captor
    private ArgumentCaptor<TransactionEvent> eventCaptor;

    @Nested
    @DisplayName("When call process with valid TransactionEvent")
    class WhenProcess {

        @Test
        @DisplayName("given valid event, then should enrich and publish returning a protocol ID")
        void shouldEnrichAndPublish() {
            var input = new TransactionEvent("key-pix-1", new BigDecimal("150.00"), "doc-123");

            var protocolId = useCase.process(input);

            verify(publishTransactionPort).publish(eventCaptor.capture());
            var published = eventCaptor.getValue();
            assertNotNull(protocolId);
            assertTrue(protocolId.length() > 0);
            assertEquals("key-pix-1", published.keyPix());
            assertEquals(new BigDecimal("150.00"), published.amount());
            assertEquals("doc-123", published.payerDocument());
            assertNotNull(published.id());
            assertNotNull(published.timestamp());
            assertEquals(TransactionStatus.RECEIVED, published.status());
        }
    }
}
