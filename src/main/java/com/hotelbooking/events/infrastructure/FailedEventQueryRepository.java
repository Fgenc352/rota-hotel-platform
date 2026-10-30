package com.hotelbooking.events.infrastructure;

import com.hotelbooking.events.application.model.FailedEvent;
import com.hotelbooking.events.application.port.FailedEventQueries;
import com.hotelbooking.events.infrastructure.entity.EventDeliveryEntity;
import com.hotelbooking.events.infrastructure.mapper.EventDeliveryEntityMapper;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FailedEventQueryRepository implements FailedEventQueries {
    private final EntityManager entityManager;
    private final EventDeliveryEntityMapper mapper;

    public List<FailedEvent> failed(UUID hotel) {
        return entityManager.createQuery("""
            select d from EventDeliveryEntity d join fetch d.event e
            where e.hotelId=:hotel and d.dead=true and d.processedAt is null order by e.createdAt,e.id
            """,
            EventDeliveryEntity.class).setParameter("hotel", hotel).setMaxResults(100).getResultList()
            .stream().map(mapper::toFailed).toList();
    }
}
