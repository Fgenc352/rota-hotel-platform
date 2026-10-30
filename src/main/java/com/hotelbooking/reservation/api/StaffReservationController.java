package com.hotelbooking.reservation.api;

import com.hotelbooking.identity.application.model.CurrentStaff;
import com.hotelbooking.reservation.api.dto.TransitionRequest;
import com.hotelbooking.reservation.api.mapper.ReservationStatusRequestMapper;
import com.hotelbooking.reservation.application.command.ChangeReservationStatusHandler;
import com.hotelbooking.reservation.application.model.BookingPage;
import com.hotelbooking.reservation.application.model.BookingSearch;
import com.hotelbooking.reservation.application.model.BookingSort;
import com.hotelbooking.reservation.application.model.BookingSummary;
import com.hotelbooking.reservation.application.model.ReservationReceipt;
import com.hotelbooking.reservation.application.query.GetBookingHandler;
import com.hotelbooking.reservation.application.query.ListBookingsHandler;
import com.hotelbooking.reservation.domain.ReservationStatus;
import com.hotelbooking.shared.application.SortDirection;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/crm")
@Validated
@RequiredArgsConstructor
public class StaffReservationController {
    private final ListBookingsHandler listBookings;
    private final GetBookingHandler getBooking;
    private final ChangeReservationStatusHandler changeReservationStatus;
    private final ReservationStatusRequestMapper reservationStatusRequestMapper;
    @GetMapping("/reservations")
    BookingPage bookings(@Parameter(hidden = true) CurrentStaff staff,
        @RequestParam(defaultValue = "0") @Min(0) @Max(100000) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue = "CREATED_AT") BookingSort sort,
        @RequestParam(defaultValue = "DESC") SortDirection direction,
        @Valid @ParameterObject @ModelAttribute BookingSearch search) {
        UUID hotel = staff.hotelId();
        return listBookings.handle(new ListBookingsHandler.Input(hotel, page, size, sort, direction, search));
    }

    @GetMapping("/reservations/{id}")
    BookingSummary detail(@Parameter(hidden = true) CurrentStaff staff, @PathVariable UUID id) {
        return getBooking.handle(staff.hotelId(), id);
    }

    @PostMapping("/reservations/{id}/status")
    ReservationReceipt transition(@Parameter(hidden = true) CurrentStaff staff, @PathVariable UUID id,
        @Valid @RequestBody TransitionRequest body) {
        UUID hotel = staff.hotelId();
        return changeReservationStatus.handle(reservationStatusRequestMapper.toCommand(hotel, id, body));
    }

    @PostMapping("/reservations/{id}/cancel")
    ReservationReceipt cancel(@Parameter(hidden = true) CurrentStaff staff, @PathVariable UUID id) {
        UUID hotel = staff.hotelId();
        return changeReservationStatus.handle(new ChangeReservationStatusHandler.Input(hotel, id,
            ReservationStatus.CANCELLED));
    }
}
