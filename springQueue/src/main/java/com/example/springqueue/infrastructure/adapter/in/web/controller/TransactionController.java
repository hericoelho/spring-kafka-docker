package com.example.springqueue.infrastructure.adapter.in.web.controller;

import com.example.springqueue.application.ports.in.ProcessTransactionUseCase;
import com.example.springqueue.domain.model.TransactionEvent;
import com.example.springqueue.infrastructure.adapter.in.web.dto.TransactionRequest;
import com.example.springqueue.infrastructure.adapter.in.web.dto.TransactionResponse;
import com.example.springqueue.infrastructure.logging.SensitiveDataMask;
import com.example.springqueue.infrastructure.mapper.TransactionEventMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final ProcessTransactionUseCase processTransactionUseCase;
    private final TransactionEventMapper transactionEventMapper;

    @PostMapping
    public ResponseEntity<TransactionResponse> processTransaction(@RequestBody @Valid TransactionRequest transactionRequest) {

        log.info("Received transaction request: keyPix={}, payerDocument={}, amount={}",
                SensitiveDataMask.mask(transactionRequest.keyPix()),
                SensitiveDataMask.mask(transactionRequest.payerDocument()),
                transactionRequest.amount());

        TransactionEvent transactionEvent = transactionEventMapper.toDomain(transactionRequest);
        String protocolId = processTransactionUseCase.process(transactionEvent);

        log.info("Transaction processed successfully: protocolId={}", protocolId);

        TransactionResponse transactionResponse = transactionEventMapper.toResponse(protocolId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(transactionResponse);
    }
}
