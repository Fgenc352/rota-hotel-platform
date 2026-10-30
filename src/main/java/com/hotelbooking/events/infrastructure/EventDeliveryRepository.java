package com.hotelbooking.events.infrastructure;

import com.hotelbooking.events.application.model.EventDelivery;
import com.hotelbooking.events.application.model.RetrySchedule;
import com.hotelbooking.events.application.port.EventDeliveries;
import com.hotelbooking.events.infrastructure.entity.EventDeliveryEntity;
import com.hotelbooking.events.infrastructure.entity.EventDeliveryId;
import com.hotelbooking.events.infrastructure.mapper.EventDeliveryEntityMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class EventDeliveryRepository implements EventDeliveries {
    private final EntityManager entityManager;
    private final EventDeliveryEntityMapper mapper;

    public Optional<EventDelivery> lockNext(String consumer) {
        // PostgreSQL SKIP LOCKED is intentionally native; Hibernate manages the selected delivery entity.
        java.util.List<?> rows = entityManager.createNativeQuery("""
            SELECT d.* FROM event_delivery d JOIN outbox_event e ON e.id=d.event_id
            WHERE d.consumer=:consumer AND d.processed_at IS NULL AND NOT d.dead AND d.next_attempt<=now()
            AND NOT EXISTS (
                SELECT 1 FROM outbox_event older JOIN event_delivery od ON od.event_id=older.id
                WHERE older.hotel_id=e.hotel_id AND older.reservation_id=e.reservation_id
                AND older.aggregate_version<e.aggregate_version AND od.consumer=d.consumer AND od.processed_at IS NULL
            ) ORDER BY e.created_at,e.id LIMIT 1 FOR UPDATE OF d SKIP LOCKED
            """,
            EventDeliveryEntity.class).setParameter("consumer", consumer).getResultList();
        return rows.stream().map(EventDeliveryEntity.class::cast).map(mapper::toModel).findFirst();
    }
    public void complete(UUID event, String consumer) {
        var entity = lock(event, consumer);
        if (entity != null && entity.getProcessedAt() == null) {
            entity.complete(Instant.now());
            entityManager.flush();
        }
    }
    public Optional<RetrySchedule> fail(UUID event, String consumer, String error) {
        var entity = lock(event, consumer);
        if (entity == null || entity.getProcessedAt() != null) {
            return Optional.empty();
        }
        entity.fail(error, Instant.now());
        entityManager.flush();
        return Optional.of(mapper.toRetry(entity));
    }
    private EventDeliveryEntity lock(UUID event, String consumer) {
        return entityManager.find(EventDeliveryEntity.class, new EventDeliveryId(event, consumer), LockModeType.PESSIMISTIC_WRITE);
    }
}
