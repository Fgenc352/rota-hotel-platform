package com.hotelbooking.events.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaAdminOperations;
import org.springframework.kafka.config.TopicBuilder;
import com.hotelbooking.events.config.MessagingProperties;
import org.springframework.stereotype.Component;

/** Retry initial topic creation through the durable outbox if the broker started late. */
@Component
@ConditionalOnProperty(name = "app.messaging.transport", havingValue = "kafka")
@RequiredArgsConstructor
public class KafkaTopicProvisioner {
    private final KafkaAdminOperations admin;
    private final MessagingProperties properties;
    private boolean ready;

    public synchronized void ensureReady() {
        if (ready) return;
        admin.createOrModifyTopics(
            TopicBuilder.name(properties.topic()).partitions(properties.partitions()).replicas(properties.replicas()).build(),
            TopicBuilder.name(properties.deadLetterTopic()).partitions(properties.partitions()).replicas(properties.replicas()).build());
        ready = true;
    }
}
