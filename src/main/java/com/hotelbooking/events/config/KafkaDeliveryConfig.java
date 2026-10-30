package com.hotelbooking.events.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessException;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.messaging.transport", havingValue = "kafka")
public class KafkaDeliveryConfig {
    @Bean
    KafkaAdmin.NewTopics reservationTopics(MessagingProperties properties) {
        return new KafkaAdmin.NewTopics(
            TopicBuilder.name(properties.topic()).partitions(properties.partitions()).replicas(properties.replicas()).build(),
            TopicBuilder.name(properties.deadLetterTopic()).partitions(properties.partitions()).replicas(properties.replicas()).build());
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<Object, Object> pmsKafkaListenerFactory(
        ConsumerFactory<Object, Object> consumers, KafkaTemplate<Object, Object> template, MessagingProperties properties
        ) {
        var recoverer = new DeadLetterPublishingRecoverer(template,
            (record, failure) -> new TopicPartition(properties.deadLetterTopic(), record.partition()));
        recoverer.setFailIfSendResultIsError(true);
        var errors = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
        errors.setBackOffFunction((record, failure) -> retryBackOff(failure));
        // DB outages are retried; malformed/unmatched envelopes are quarantined in the DLT.
        var factory = new ConcurrentKafkaListenerContainerFactory<Object, Object>();
        factory.setConsumerFactory(consumers);
        factory.setCommonErrorHandler(errors);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }
    static org.springframework.util.backoff.BackOff retryBackOff(Throwable failure) {
        // @Repository translates invalid envelope arguments to API-usage errors, not DB outages.
        // Check the full cause chain before classifying transaction/persistence wrappers as retryable.
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.springframework.dao.InvalidDataAccessApiUsageException) {
                return new FixedBackOff(1000L, 2L);
            }
        }
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof DataAccessException || cause instanceof org.springframework.transaction.TransactionException) {
                return new FixedBackOff(1000L, FixedBackOff.UNLIMITED_ATTEMPTS);
            }
        }
        return new FixedBackOff(1000L, 2L);
    }

}
