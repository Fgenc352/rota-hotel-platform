package com.hotelbooking.events.application.mapper;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.events.application.model.PmsReservationMessage;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PmsMessageMapper {
    public PmsReservationMessage toMessage(UUID event, ReservationSnapshot snapshot) {
        return new PmsReservationMessage(event, snapshot.hotelId(), snapshot.id(), snapshot.code(),
            snapshot.checkIn(), snapshot.checkOut(), snapshot.status(), snapshot.version(), snapshot.checkedOutAt());
    }
}
