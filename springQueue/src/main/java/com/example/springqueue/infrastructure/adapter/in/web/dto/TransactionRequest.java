package com.example.springqueue.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotBlank
        String keyPix,
        @NotNull
        @DecimalMin("0")
        BigDecimal amount,
        @NotBlank
        String payerDocument
) {
}
