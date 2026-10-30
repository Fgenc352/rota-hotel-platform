package com.hotelbooking.events.application;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import java.util.UUID;

/** Boundary for delivering a reservation to the property management system. */ public interface PmsGateway {
    void deliver(UUID eventId, String traceId, ReservationSnapshot snapshot);
}
