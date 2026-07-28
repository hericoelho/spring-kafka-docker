package com.example.springqueue.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.MicrometerConsumerListener;
import org.springframework.kafka.core.MicrometerProducerListener;

@Configuration(proxyBeanMethods = false)
public class KafkaMetricsConfig {

    @Bean
    DefaultKafkaConsumerFactoryCustomizer consumerMetricsCustomizer(MeterRegistry meterRegistry) {
        return cf -> cf.addListener(new MicrometerConsumerListener<>(meterRegistry));
    }

    @Bean
    DefaultKafkaProducerFactoryCustomizer producerMetricsCustomizer(MeterRegistry meterRegistry) {
        return pf -> pf.addListener(new MicrometerProducerListener<>(meterRegistry));
    }
}