package com.hotelbooking.reservation.application.port;

import com.hotelbooking.reservation.domain.ServiceLine;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ReservationExtras {
    void save(UUID hotel, UUID reservation, BigDecimal roomTotal, List<ServiceLine> lines);
}
