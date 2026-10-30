package com.hotelbooking.pms.infrastructure;

import com.hotelbooking.pms.application.model.DailyServiceTask;
import com.hotelbooking.pms.application.model.DailyServiceAudit;
import com.hotelbooking.pms.application.port.DailyServiceTasks;
import com.hotelbooking.pms.infrastructure.entity.PmsDailyServiceEntity;
import com.hotelbooking.pms.infrastructure.entity.PmsDailyServiceAuditEntity;
import com.hotelbooking.pms.infrastructure.mapper.DailyServiceMapper;
import com.hotelbooking.shared.json.Json;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
@RequiredArgsConstructor
public class DailyServiceRepository implements DailyServiceTasks {
    private final EntityManager em;
    private final DailyServiceMapper mapper;
    private final Json json;

    public List<DailyServiceTask> tasks(UUID hotel, Collection<UUID> reservations, LocalDate date) {
        if (reservations.isEmpty()) return List.of();
        return em.createQuery("select t from PmsDailyServiceEntity t where t.hotelId=:hotel and t.reservationId in :ids and (t.date=:date or t.code='TRANSFER')",
            PmsDailyServiceEntity.class)
            .setParameter("hotel", hotel).setParameter("ids", reservations).setParameter("date", date)
            .getResultList().stream().map(mapper::task).toList();
    }

    public void save(UUID hotel, DailyServiceTask value) {
        // Caller holds the source reservation lock, so creation and rescheduling are serialized.
        var found = em.createQuery("select t from PmsDailyServiceEntity t where t.hotelId=:hotel and t.reservationId=:reservation and t.code=:code and (t.date=:date or t.code='TRANSFER')",
            PmsDailyServiceEntity.class)
            .setParameter("hotel", hotel).setParameter("reservation", value.reservationId()).setParameter("code",
            value.code())
            .setParameter("date", value.date()).getResultList();
        Object before = found.isEmpty() ? Map.of() : mapper.task(found.getFirst());
        if (found.isEmpty()) {
            var entity = PmsDailyServiceEntity.builder().id(UUID.randomUUID()).hotelId(hotel).reservationId(value.reservationId()).code(value.code()).build();
            entity.update(value);
            em.persist(entity);
        } else found.getFirst().update(value);
        em.persist(PmsDailyServiceAuditEntity.builder().id(UUID.randomUUID()).hotelId(hotel).reservationId(value.reservationId())
            .code(value.code()).date(value.date()).actor(value.actor()).beforeState(json.write(before)).afterState(json.write(value)).createdAt(Instant.now()).build());
        em.flush();
    }

    public List<DailyServiceAudit> history(UUID hotel, UUID reservation, String code, LocalDate date) {
        return em.createQuery("select a from PmsDailyServiceAuditEntity a where a.hotelId=:hotel and a.reservationId=:reservation and a.code=:code and (a.date=:date or a.code='TRANSFER') order by a.createdAt desc,a.id",
            PmsDailyServiceAuditEntity.class)
            .setParameter("hotel", hotel).setParameter("reservation", reservation).setParameter("code", code).setParameter("date",
            date)
            .setMaxResults(100).getResultList().stream().map(mapper::audit).toList();
    }
}
