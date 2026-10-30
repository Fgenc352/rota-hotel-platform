package com.hotelbooking.reservation.infrastructure;

import com.hotelbooking.reservation.application.port.ReservationExtras;
import com.hotelbooking.reservation.domain.ServiceLine;
import com.hotelbooking.reservation.infrastructure.jpa.ReservationJpaRepository;
import com.hotelbooking.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReservationExtrasRepository implements ReservationExtras {
    private final ReservationJpaRepository reservations;
    public void save(UUID hotel, UUID reservation, BigDecimal roomTotal, List<ServiceLine> lines) {
        var entity = reservations.findByIdAndHotelId(reservation,
            hotel).orElseThrow(BusinessException::missing);
        entity.recordServices(roomTotal, lines);
    }
}
