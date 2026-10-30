package com.hotelbooking.reservation.infrastructure;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.reservation.application.port.ReservationSnapshots;
import com.hotelbooking.reservation.infrastructure.jpa.ReservationJpaRepository;
import com.hotelbooking.reservation.infrastructure.mapper.ReservationEntityMapper;
import com.hotelbooking.shared.exception.BusinessException;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class ReservationSnapshotRepository implements ReservationSnapshots {
    private final EntityManager entityManager;
    private final ReservationJpaRepository reservations;
    private final ReservationEntityMapper mapper;

    public ReservationSnapshot get(UUID hotel, UUID id) {
        entityManager.flush();
        return mapper.toSnapshot(reservations.findByIdAndHotelId(id, hotel).orElseThrow(BusinessException::missing));
    }
}
