package com.example.springqueue.infrastructure.mapper;

import com.example.springqueue.domain.model.TransactionEvent;
import com.example.springqueue.infrastructure.adapter.in.web.dto.TransactionRequest;
import com.example.springqueue.infrastructure.adapter.in.web.dto.TransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventMapper {
    public TransactionEvent toDomain(TransactionRequest transactionRequest) {
        return new TransactionEvent(
                transactionRequest.keyPix(),
                transactionRequest.amount(),
                transactionRequest.payerDocument()
        );
    }

    public TransactionResponse toResponse(String protocolId){
        return new TransactionResponse(protocolId);
    }
}
