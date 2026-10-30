package com.hotelbooking.reservation.api;

import com.hotelbooking.reservation.api.dto.BookingRequest;
import com.hotelbooking.reservation.api.dto.FunnelRequest;
import com.hotelbooking.reservation.api.mapper.BookingRequestMapper;
import com.hotelbooking.reservation.api.mapper.FunnelRequestMapper;
import com.hotelbooking.reservation.application.command.CreateReservationHandler;
import com.hotelbooking.reservation.application.command.RecordFunnelHandler;
import com.hotelbooking.reservation.application.model.ReservationReceipt;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/hotels/{key}")
@RequiredArgsConstructor
public class ReservationController {
    private final CreateReservationHandler create;
    private final RecordFunnelHandler funnel;
    private final BookingRequestMapper bookingRequestMapper;
    private final FunnelRequestMapper funnelRequestMapper;

    @PostMapping("/reservations")
    ResponseEntity<ReservationReceipt> book(@PathVariable String key, @RequestHeader("Idempotency-Key") String idempotency,
        @Valid @RequestBody BookingRequest request) {
        var command = bookingRequestMapper.toCommand(key, idempotency, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(create.handle(command));
    }

    @PostMapping("/funnel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void funnel(@PathVariable String key, @Valid @RequestBody FunnelRequest event) {
        funnel.handle(funnelRequestMapper.toCommand(key, event));
    }
}
