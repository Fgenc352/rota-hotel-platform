package com.hotelbooking.pms.application.query;

import com.hotelbooking.pms.application.model.ReservationServices;
import com.hotelbooking.pms.application.model.ServiceTaskView;
import com.hotelbooking.pms.application.port.ServiceTasks;
import com.hotelbooking.pms.domain.ServiceProgress;
import com.hotelbooking.reservation.application.query.GetBookingHandler;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetReservationServicesHandler {
    private final GetBookingHandler bookings;
    private final ServiceTasks tasks;

    public ReservationServices handle(UUID hotel, UUID reservation) {
        var booking = bookings.handle(hotel, reservation);
        var states = tasks.states(hotel, reservation).stream().collect(Collectors.toMap(state -> state.code(),
            state -> state));
        var services = booking.services().stream().map(line -> {
            var state = states.get(line.code());
            return new ServiceTaskView(line, state == null ? ServiceProgress.PENDING : state.progress(), !line.code().equals("WIFI"),
                state == null ? null : state.actor(), state == null ? null : state.updatedAt());
        }).toList();
        return new ReservationServices(reservation, booking.code(), booking.status(), services, tasks.history(hotel,
            reservation));
    }
}
