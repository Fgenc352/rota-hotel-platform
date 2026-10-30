package com.hotelbooking.reservation.api.mapper;

import com.hotelbooking.reservation.api.dto.TransitionRequest;
import com.hotelbooking.reservation.application.command.ChangeReservationStatusHandler;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ReservationStatusRequestMapper {
    public ChangeReservationStatusHandler.Input toCommand(UUID hotel, UUID reservation, TransitionRequest request) {
        return new ChangeReservationStatusHandler.Input(hotel, reservation, request.status());
    }
}
