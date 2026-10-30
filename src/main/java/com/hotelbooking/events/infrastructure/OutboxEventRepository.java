package com.hotelbooking.events.infrastructure;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.events.application.port.EventOutbox;
import com.hotelbooking.events.config.MessagingProperties;
import com.hotelbooking.events.infrastructure.entity.EventDeliveryEntity;
import com.hotelbooking.events.infrastructure.entity.EventDeliveryId;
import com.hotelbooking.events.infrastructure.entity.OutboxEventEntity;
import com.hotelbooking.shared.json.Json;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class OutboxEventRepository implements EventOutbox {
    private final EntityManager entityManager;
    private final Json json;
    private final MessagingProperties messaging;

    public void append(UUID event, UUID hotel, UUID id, String type, ReservationSnapshot snapshot, String traceId) {
        entityManager.persist(OutboxEventEntity.builder().id(event).hotelId(hotel).reservationId(id)
            .aggregateVersion(snapshot.version()).eventType(type).schemaVersion(1).payload(json.write(snapshot))
            .traceId(traceId).createdAt(Instant.now()).build());
        entityManager.flush();
        for (String consumer : new String[] {
            "CRM", messaging.transport() == MessagingProperties.Transport.KAFKA ? "KAFKA" : "PMS"
        }) {
            entityManager.persist(EventDeliveryEntity.builder().id(new EventDeliveryId(event, consumer)).nextAttempt(Instant.now()).build());
        }
    }
}
