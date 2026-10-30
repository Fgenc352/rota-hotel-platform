package com.hotelbooking.reservation.application.port;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import java.util.UUID;

public interface ReservationSnapshots {
    ReservationSnapshot get(UUID hotel, UUID id);
}
