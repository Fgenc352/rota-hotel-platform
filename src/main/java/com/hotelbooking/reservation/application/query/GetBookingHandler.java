package com.hotelbooking.reservation.application.query;

import com.hotelbooking.reservation.application.model.BookingSummary;
import com.hotelbooking.reservation.application.port.BookingQueries;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetBookingHandler {
    private final BookingQueries bookings;
    public BookingSummary handle(UUID hotel, UUID reservation) {
        return bookings.detail(hotel, reservation);
    }
}
