package com.hotelbooking.events.infrastructure;

import com.hotelbooking.events.application.port.EventRetries;
import com.hotelbooking.events.infrastructure.entity.EventDeliveryEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class EventRetryRepository implements EventRetries {
    private final EntityManager entityManager;

    public boolean retry(UUID hotel, UUID id) {
        var pending = entityManager.createQuery("""
            select d from EventDeliveryEntity d join d.event e
            where e.id=:id and e.hotelId=:hotel and d.processedAt is null
            """,
            EventDeliveryEntity.class).setParameter("id", id).setParameter("hotel", hotel)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        pending.forEach(delivery -> delivery.retry(Instant.now()));
        return !pending.isEmpty();
    }
}
