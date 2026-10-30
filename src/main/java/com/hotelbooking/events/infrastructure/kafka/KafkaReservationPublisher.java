package com.hotelbooking.events.infrastructure.kafka;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.events.application.EventConsumer;
import com.hotelbooking.events.application.model.BrokerReservationEvent;
import com.hotelbooking.events.config.MessagingProperties;
import com.hotelbooking.shared.json.Json;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.transport", havingValue = "kafka")
@RequiredArgsConstructor
@Slf4j
public class KafkaReservationPublisher implements EventConsumer {
    private final KafkaTemplate<Object, Object> kafka;
    private final Json json;
    private final MessagingProperties properties;
    private final KafkaTopicProvisioner topics;

    public String name() {
        return "KAFKA";
    }
    public boolean requiresLocalTransaction() {
        return false;
    }

    public void accept(UUID event, String type, String traceId, ReservationSnapshot snapshot) {
        var message = new BrokerReservationEvent(event, snapshot.hotelId(), snapshot.id(), snapshot.version(),
            type, traceId);
        try {
            topics.ensureReady();
            // Ordering is by reservation. DB outbox acknowledgement follows the broker acknowledgement.
            var result = kafka.send(properties.topic(), snapshot.id().toString(), json.write(message))
                .get(properties.publishTimeout().toMillis(), TimeUnit.MILLISECONDS);
            log.info("event=kafka.published eventId={} topic={} partition={} offset={}", event,
                result.getRecordMetadata().topic(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka publish interrupted", interrupted);
        } catch (Exception failure) {
            throw new IllegalStateException("Kafka publish failed", failure);
        }
    }
}
