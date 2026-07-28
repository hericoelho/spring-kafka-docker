package com.example.springqueue.infrastructure.mapper;

import com.example.springqueue.domain.model.TransactionEvent;
import com.example.springqueue.infrastructure.adapter.in.web.dto.TransactionRequest;
import com.example.springqueue.infrastructure.adapter.in.web.dto.TransactionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Given a TransactionEventMapper")
class TransactionEventMapperTest {

    private final TransactionEventMapper mapper = new TransactionEventMapper();

    @Nested
    @DisplayName("When call toDomain with valid TransactionRequest")
    class WhenToDomain {

        @Test
        @DisplayName("given all fields present, then should map fields correctly")
        void shouldMapAllFields() {
            var request = new TransactionRequest("chave-pix-123", new BigDecimal("99.99"), "123.456.789-00");

            var result = mapper.toDomain(request);

            assertEquals("chave-pix-123", result.keyPix());
            assertEquals(new BigDecimal("99.99"), result.amount());
            assertEquals("123.456.789-00", result.payerDocument());
            assertNull(result.id());
            assertNull(result.timestamp());
            assertNull(result.status());
        }
    }

    @Nested
    @DisplayName("When call toResponse with protocolId")
    class WhenToResponse {

        @Test
        @DisplayName("given a valid protocolId, then should wrap in response")
        void shouldWrapProtocolId() {
            var protocolId = "protocol-abc-123";

            var result = mapper.toResponse(protocolId);

            assertEquals(protocolId, result.protocolId());
        }
    }
}
