package com.hotelbooking.pms.infrastructure;

import com.hotelbooking.pms.application.model.RoomAudit;
import com.hotelbooking.pms.application.model.RoomAuditPage;
import com.hotelbooking.pms.application.model.RoomView;
import com.hotelbooking.pms.application.port.RoomBoardQueries;
import com.hotelbooking.pms.domain.RoomAssignment;
import com.hotelbooking.pms.infrastructure.entity.PmsAssignmentEntity;
import com.hotelbooking.pms.infrastructure.entity.PmsRoomAuditEntity;
import com.hotelbooking.pms.infrastructure.entity.PmsRoomEntity;
import com.hotelbooking.pms.infrastructure.mapper.PmsRoomMapper;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PmsRoomQueryRepository implements RoomBoardQueries {
    private final EntityManager em;
    private final PmsRoomMapper mapper;

    public List<RoomAssignment> assignments(UUID hotel) {
        return em.createQuery("select a from PmsAssignmentEntity a where a.hotelId=:hotel and a.status in ('CONFIRMED','CHECKED_IN')",
            PmsAssignmentEntity.class)
            .setParameter("hotel", hotel).getResultList().stream().map(mapper::assignment).toList();
    }

    public List<RoomAssignment> dailyAssignments(UUID hotel, java.time.LocalDate date) {
        return em.createQuery("select a from PmsAssignmentEntity a where a.hotelId=:hotel and (a.status in ('CONFIRMED','CHECKED_IN') or (a.status='CHECKED_OUT' and a.checkIn<=:day and a.checkOut>=:day))",
            PmsAssignmentEntity.class)
            .setParameter("hotel", hotel).setParameter("day", date).getResultList().stream().map(mapper::assignment).toList();
    }

    public List<RoomView> rooms(UUID hotel) {
        var assignments = assignments(hotel);
        return em.createQuery("select r from PmsRoomEntity r where r.hotelId=:hotel order by r.number", PmsRoomEntity.class)
            .setParameter("hotel", hotel).getResultList().stream().map(room -> {
            var occupied = assignments.stream().filter(a -> a.roomId().equals(room.getId()) && a.status().equals("CHECKED_IN")).findFirst();
            long planned = assignments.stream().filter(a -> a.roomId().equals(room.getId()) && a.status().equals("CONFIRMED")).count();
            return new RoomView(room.getId(), room.getRoomTypeId(), "", room.getNumber(), room.getHousekeeping(),
                occupied.map(RoomAssignment::reservationId).orElse(null), occupied.map(RoomAssignment::code).orElse(null),
                planned);
        }).toList();
    }

    public RoomAuditPage historyPage(UUID hotel, int page, int size) {
        long total = em.createQuery("select count(a) from PmsRoomAuditEntity a where a.hotelId=:hotel", Long.class)
            .setParameter("hotel", hotel).getSingleResult();
        return new RoomAuditPage(history(hotel, page * size, size), total, page, size);
    }

    private List<RoomAudit> history(UUID hotel, int offset, int size) {
        var query = em.createQuery("select a from PmsRoomAuditEntity a where a.hotelId=:hotel order by a.createdAt desc,a.id",
            PmsRoomAuditEntity.class)
            .setParameter("hotel", hotel).setFirstResult(offset).setMaxResults(size);
        var numbers = em.createQuery("select r from PmsRoomEntity r where r.hotelId=:hotel", PmsRoomEntity.class)
            .setParameter("hotel", hotel).getResultList().stream().collect(java.util.stream.Collectors.toMap(PmsRoomEntity::getId,
            PmsRoomEntity::getNumber));
        return query.getResultList().stream().map(row -> {
            return new RoomAudit(row.getId(), row.getRoomId(), row.getReservationId(), numbers.get(row.getRoomId()),
                row.getAction(), row.getActor(),
                row.getBeforeState(), row.getAfterState(), row.getCreatedAt());
        }).toList();
    }
}
