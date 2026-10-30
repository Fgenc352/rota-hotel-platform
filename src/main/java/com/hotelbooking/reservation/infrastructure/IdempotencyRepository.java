package com.hotelbooking.reservation.infrastructure;

import com.hotelbooking.reservation.application.model.IdempotencyEntry;
import com.hotelbooking.reservation.application.port.IdempotencyRecords;
import com.hotelbooking.reservation.infrastructure.entity.IdempotencyEntity;
import com.hotelbooking.reservation.infrastructure.entity.IdempotencyId;
import com.hotelbooking.reservation.infrastructure.mapper.IdempotencyEntityMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class IdempotencyRepository implements IdempotencyRecords {
    private final EntityManager entityManager;
    private final IdempotencyEntityMapper mapper;

    public IdempotencyEntry lock(UUID hotel, String key, String hash) {
        entityManager.flush();
        entityManager.createNativeQuery("""
            INSERT INTO idempotency_record(hotel_id,key,request_hash) VALUES (:hotel,:key,:hash)
            ON CONFLICT DO NOTHING
            """).setParameter("hotel",
            hotel).setParameter("key", key).setParameter("hash", hash).executeUpdate();
        return mapper.toModel(entityManager.find(IdempotencyEntity.class, new IdempotencyId(hotel, key), LockModeType.PESSIMISTIC_WRITE));
    }
    public void complete(UUID hotel, String key, String response) {
        entityManager.find(IdempotencyEntity.class, new IdempotencyId(hotel, key)).complete(response);
    }
}
