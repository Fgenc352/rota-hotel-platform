package com.hotelbooking.reservation.application.port;

import com.hotelbooking.inventory.domain.Room;
import com.hotelbooking.reservation.application.command.BookingData;
import com.hotelbooking.reservation.domain.ReservationState;
import com.hotelbooking.reservation.domain.ReservationStatus;
import com.hotelbooking.shared.domain.Stay;
import java.math.BigDecimal;
import java.util.UUID;

public interface Reservations {
    UUID roomId(UUID hotel, UUID id);

    ReservationState lock(UUID hotel, UUID id);

    void insert(UUID id, UUID hotel, String code, BookingData request, BigDecimal total);

    void insertNights(UUID hotel, UUID id, Room room, Stay stay);

    void changeStatus(UUID hotel, UUID id, ReservationStatus next);
}
