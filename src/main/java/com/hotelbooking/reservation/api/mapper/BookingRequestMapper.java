package com.hotelbooking.reservation.api.mapper;

import com.hotelbooking.reservation.api.dto.BookingRequest;
import com.hotelbooking.reservation.application.command.BookingData;
import com.hotelbooking.reservation.application.command.CreateReservationHandler;
import org.springframework.stereotype.Component;

@Component
public class BookingRequestMapper {
    public CreateReservationHandler.Input toCommand(String hotelKey, String idempotencyKey,
        BookingRequest request) {
        var booking = new BookingData(
            request.roomTypeId(), request.checkIn(), request.checkOut(), request.adults(), request.children(),
            request.firstName(), request.lastName(), request.email(), request.phone(),
            request.privacyAccepted(), request.marketingAllowed(), request.sessionId(), request.serviceCodes());
        return new CreateReservationHandler.Input(hotelKey, idempotencyKey, booking);
    }
}
