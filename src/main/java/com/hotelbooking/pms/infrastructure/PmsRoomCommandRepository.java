package com.hotelbooking.pms.infrastructure;

import com.hotelbooking.pms.application.port.RoomOperations;
import com.hotelbooking.pms.domain.Housekeeping;
import com.hotelbooking.pms.domain.PhysicalRoom;
import com.hotelbooking.pms.domain.RoomAssignment;
import com.hotelbooking.pms.infrastructure.entity.PmsAssignmentEntity;
import com.hotelbooking.pms.infrastructure.entity.PmsRoomAuditEntity;
import com.hotelbooking.pms.infrastructure.entity.PmsRoomEntity;
import com.hotelbooking.pms.infrastructure.mapper.PmsRoomMapper;
import com.hotelbooking.shared.exception.BusinessException;
import com.hotelbooking.shared.json.Json;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
@RequiredArgsConstructor
public class PmsRoomCommandRepository implements RoomOperations {
    private final EntityManager em;
    private final PmsRoomMapper mapper;
    private final Json json;

    private PmsRoomEntity room(UUID hotel, UUID id) {
        return em.createQuery("select r from PmsRoomEntity r where r.hotelId=:hotel and r.id=:id", PmsRoomEntity.class)
            .setParameter("hotel", hotel).setParameter("id", id).setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getResultList().stream().findFirst().orElseThrow(BusinessException::missing);
    }

    private Optional<PmsAssignmentEntity> assigned(UUID hotel, UUID reservation) {
        var entity = em.find(PmsAssignmentEntity.class, reservation);
        return entity != null && entity.getHotelId().equals(hotel) ? Optional.of(entity) : Optional.empty();
    }

    public PhysicalRoom lockRoom(UUID hotel, UUID id) {
        return mapper.room(room(hotel, id));
    }
    public Optional<RoomAssignment> assignment(UUID hotel, UUID reservation) {
        return assigned(hotel, reservation).map(mapper::assignment);
    }

    public boolean conflicts(UUID hotel, UUID room, UUID exclude, LocalDate in, LocalDate out, boolean checkingIn) {
        return em.createQuery("""
            select count(a) from PmsAssignmentEntity a where a.hotelId=:hotel and a.roomId=:room
            and a.reservationId<>:exclude and a.status in ('CONFIRMED','CHECKED_IN')
            and ((a.checkIn<:end and a.checkOut>:start) or (:checkingIn=true and a.status='CHECKED_IN'))
            """,
            Long.class).setParameter("hotel", hotel).setParameter("room", room).setParameter("exclude", exclude)
            .setParameter("start", in).setParameter("end", out).setParameter("checkingIn", checkingIn).getSingleResult() > 0;
    }

    public void assign(UUID hotel, RoomAssignment value) {
        var existing = assigned(hotel, value.reservationId());
        if (existing.isPresent()) {
            existing.get().move(value.roomId(), value.checkIn(), value.checkOut(), value.status());
        } else {
            em.persist(PmsAssignmentEntity.builder().reservationId(value.reservationId()).hotelId(hotel).roomId(value.roomId())
                .code(value.code()).checkIn(value.checkIn()).checkOut(value.checkOut()).status(value.status()).build());
        }
        em.flush();
    }

    public void transition(UUID hotel, UUID reservation, String status) {
        assigned(hotel, reservation).orElseThrow(BusinessException::missing).changeStatus(status);
        em.flush();
    }

    public void housekeeping(UUID hotel, UUID id, Housekeeping state) {
        room(hotel, id).changeHousekeeping(state);
        em.flush();
    }

    public void audit(UUID hotel, UUID room, UUID reservation, String action, String actor, Object before,
        Object after) {
        em.persist(PmsRoomAuditEntity.builder().id(UUID.randomUUID()).hotelId(hotel).roomId(room).reservationId(reservation)
            .action(action).actor(actor).beforeState(json.write(before)).afterState(json.write(after)).createdAt(Instant.now()).build());
    }
}
