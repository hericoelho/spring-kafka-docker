package com.example.springqueue.infrastructure.adapter.in.web.controller;

import com.example.springqueue.application.ports.in.ProcessTransactionUseCase;
import com.example.springqueue.domain.model.TransactionEvent;
import com.example.springqueue.infrastructure.adapter.in.web.dto.TransactionResponse;
import com.example.springqueue.infrastructure.mapper.TransactionEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import(TransactionControllerTest.MockConfiguration.class)
@DisplayName("Given a TransactionController")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProcessTransactionUseCase processTransactionUseCase;

    @Autowired
    private TransactionEventMapper transactionEventMapper;

    @BeforeEach
    void setUp() {
        var inputEvent = new TransactionEvent("chave-pix-123", new BigDecimal("99.99"), "123.456.789-00");
        given(transactionEventMapper.toDomain(any())).willReturn(inputEvent);
        given(transactionEventMapper.toResponse("protocol-abc-123"))
                .willReturn(new TransactionResponse("protocol-abc-123"));
        given(processTransactionUseCase.process(any())).willReturn("protocol-abc-123");
    }

    @Nested
    @DisplayName("When POST /api/v1/transactions with valid body")
    class WhenValidRequest {

        @Test
        @DisplayName("given valid request, then should return 202 Accepted with protocolId")
        void shouldReturn202WithProtocolId() throws Exception {
            var requestBody = """
                    {
                        "keyPix": "chave-pix-123",
                        "amount": 99.99,
                        "payerDocument": "123.456.789-00"
                    }
                    """;

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.protocolId").value("protocol-abc-123"));
        }
    }

    @Nested
    @DisplayName("When POST /api/v1/transactions with invalid body")
    class WhenInvalidRequest {

        @Test
        @DisplayName("given empty keyPix, then should return 400 Bad Request")
        void shouldReturn400WhenKeyPixIsBlank() throws Exception {
            var requestBody = """
                    {
                        "keyPix": "",
                        "amount": 99.99,
                        "payerDocument": "123.456.789-00"
                    }
                    """;

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MockConfiguration {
        @Bean
        ProcessTransactionUseCase processTransactionUseCase() {
            return Mockito.mock(ProcessTransactionUseCase.class);
        }

        @Bean
        TransactionEventMapper transactionEventMapper() {
            return Mockito.mock(TransactionEventMapper.class);
        }
    }
}
