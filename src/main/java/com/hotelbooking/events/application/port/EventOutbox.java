package com.hotelbooking.events.application.port;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import java.util.UUID;

public interface EventOutbox {
    void append(UUID event, UUID hotel, UUID id, String type, ReservationSnapshot snapshot, String traceId);
}
