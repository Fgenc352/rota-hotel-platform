package com.hotelbooking.events.infrastructure.kafka;

import com.hotelbooking.events.application.model.BrokerReservationEvent;
import com.hotelbooking.events.application.port.PmsEventInbox;
import com.hotelbooking.events.infrastructure.entity.OutboxEventEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(name = "app.messaging.transport", havingValue = "kafka")
@RequiredArgsConstructor
public class KafkaPmsInboxRepository implements PmsEventInbox {
    private final EntityManager entityManager;

    @Override
    @Transactional
    public boolean stage(BrokerReservationEvent message) {
        var stored = entityManager.find(OutboxEventEntity.class, message.eventId());
        if (stored == null || !stored.getHotelId().equals(message.hotelId())
            || !stored.getReservationId().equals(message.reservationId())
            || stored.getAggregateVersion() != message.aggregateVersion()
            || !stored.getEventType().equals(message.type()) || !stored.getTraceId().equals(message.traceId())) {
            throw new IllegalArgumentException("Broker event does not match the durable outbox");
        }
        // A replay cannot create another delivery or revive an already completed/dead one.
        return entityManager.createNativeQuery("""
            INSERT INTO event_delivery(event_id,consumer,attempts,next_attempt,dead)
            VALUES (:id,'PMS',0,now(),false)
            ON CONFLICT (event_id,consumer) DO NOTHING
            """).setParameter("id",
            message.eventId()).executeUpdate() == 1;
    }
}
