package com.example.springqueue.infrastructure.config;

import com.example.springqueue.application.ports.in.ProcessTransactionUseCase;
import com.example.springqueue.application.ports.in.ReceiveTransactionUseCase;
import com.example.springqueue.application.ports.out.IdempotencyPort;
import com.example.springqueue.application.ports.out.PublishTransactionPort;
import com.example.springqueue.application.usecases.ProcessTransactionUseCaseImpl;
import com.example.springqueue.application.usecases.ReceiveTransactionUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionConfig {

    @Bean
    ProcessTransactionUseCase processTransactionUseCase(PublishTransactionPort publishTransactionPort) {
        return new ProcessTransactionUseCaseImpl(publishTransactionPort);
    }

    @Bean
    ReceiveTransactionUseCase receiveTransactionUseCase(IdempotencyPort idempotencyPort) {
        return new ReceiveTransactionUseCaseImpl(idempotencyPort);
    }
}
