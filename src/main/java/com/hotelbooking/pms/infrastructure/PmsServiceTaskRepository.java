package com.hotelbooking.pms.infrastructure;

import com.hotelbooking.pms.application.model.ServiceTaskAudit;
import com.hotelbooking.pms.application.model.ServiceTaskState;
import com.hotelbooking.pms.application.port.ServiceTasks;
import com.hotelbooking.pms.infrastructure.entity.PmsServiceAuditEntity;
import com.hotelbooking.pms.infrastructure.entity.PmsServiceTaskEntity;
import com.hotelbooking.pms.infrastructure.mapper.PmsServiceMapper;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PmsServiceTaskRepository implements ServiceTasks {
    private final EntityManager em;
    private final PmsServiceMapper mapper;

    public List<ServiceTaskState> states(UUID hotel, UUID reservation) {
        return em.createQuery("select t from PmsServiceTaskEntity t where t.hotelId=:hotel and t.reservationId=:reservation",
            PmsServiceTaskEntity.class)
            .setParameter("hotel", hotel).setParameter("reservation", reservation).getResultList().stream()
            .map(mapper::state).toList();
    }
    public List<ServiceTaskAudit> history(UUID hotel, UUID reservation) {
        return em.createQuery("select a from PmsServiceAuditEntity a where a.hotelId=:hotel and a.reservationId=:reservation order by a.createdAt desc,a.id",
            PmsServiceAuditEntity.class)
            .setParameter("hotel", hotel).setParameter("reservation", reservation).setMaxResults(100).getResultList().stream()
            .map(mapper::audit).toList();
    }
}
