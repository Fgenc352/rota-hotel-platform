package com.hotelbooking.reservation.infrastructure;

import com.hotelbooking.inventory.domain.Room;
import com.hotelbooking.reservation.application.command.BookingData;
import com.hotelbooking.reservation.application.port.Reservations;
import com.hotelbooking.reservation.domain.ReservationState;
import com.hotelbooking.reservation.domain.ReservationStatus;
import com.hotelbooking.reservation.infrastructure.entity.ReservationNightEntity;
import com.hotelbooking.reservation.infrastructure.entity.ReservationNightId;
import com.hotelbooking.reservation.infrastructure.jpa.ReservationJpaRepository;
import com.hotelbooking.reservation.infrastructure.mapper.ReservationEntityMapper;
import com.hotelbooking.shared.domain.Stay;
import com.hotelbooking.shared.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class ReservationRepository implements Reservations {
    private final EntityManager entityManager;
    private final ReservationJpaRepository reservations;
    private final ReservationEntityMapper mapper;

    public UUID roomId(UUID hotel, UUID id) {
        // Scalar lookup avoids caching a stale ReservationEntity before waiting for the room lock.
        return reservations.findRoomIdByHotelAndId(hotel, id).orElseThrow(BusinessException::missing);
    }
    public ReservationState lock(UUID hotel, UUID id) {
        return mapper.toState(reservations.lockByHotelAndId(hotel, id).orElseThrow(BusinessException::missing));
    }
    public void insert(UUID id, UUID hotel, String code, BookingData request, BigDecimal total) {
        entityManager.persist(mapper.toEntity(id, hotel, code, request, total));
    }
    public void insertNights(UUID hotel, UUID id, Room room, Stay stay) {
        for (var date : stay.dates()) {
            entityManager.persist(ReservationNightEntity.builder().id(new ReservationNightId(hotel, id, date)).price(room.price(date)).build());
        }
    }
    public void changeStatus(UUID hotel, UUID id, ReservationStatus next) {
        var entity = reservations.findByIdAndHotelId(id, hotel).orElseThrow(BusinessException::missing);
        entity.changeStatus(next.name(), Instant.now());
        entityManager.flush();
    }
}
