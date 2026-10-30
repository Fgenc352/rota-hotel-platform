package com.hotelbooking.reservation.application.command;

import com.hotelbooking.hotel.application.port.HotelCatalog;
import com.hotelbooking.contracts.pms.RoomStayLifecycle;
import com.hotelbooking.inventory.application.port.InventoryCommands;
import com.hotelbooking.reservation.application.model.ReservationReceipt;
import com.hotelbooking.reservation.application.port.Reservations;
import com.hotelbooking.reservation.domain.ReservationStatus;
import com.hotelbooking.reservation.event.ReservationEventPublisher;
import com.hotelbooking.shared.logging.TransactionLog;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChangeReservationStatusHandler {
    public record Input(UUID hotel, UUID id, ReservationStatus status) {
    }

    private final Reservations reservations;
    private final InventoryCommands inventory;
    private final HotelCatalog hotels;
    private final ReservationEventPublisher events;
    private final RoomStayLifecycle roomLifecycle;

    @Transactional
    public ReservationReceipt handle(Input input) {
        UUID room = reservations.roomId(input.hotel(), input.id());
        inventory.room(input.hotel(), room);
        var state = reservations.lock(input.hotel(), input.id());
        var receipt = new ReservationReceipt(input.id(), state.code(), input.status().name(),
            state.total(), "TRY");
        if (state.status() == input.status()) {
            return receipt;
        }
        state.validateTransition(input.status(), LocalDate.now(ZoneId.of(hotels.timezone(input.hotel()))));
        roomLifecycle.transition(input.hotel(), input.id(), input.status().name());
        if (input.status().releasesInventory()) {
            inventory.release(input.hotel(), room, state.checkIn(), state.checkOut());
        }
        reservations.changeStatus(input.hotel(), input.id(), input.status());
        events.publish(input.hotel(), input.id(), input.status() == ReservationStatus.CANCELLED ? "ReservationCancelled" : "ReservationStatusChanged");
        TransactionLog.afterCommit(() -> log.info("event=reservation.status_changed hotelId={} reservationId={} previous={} current={}",
            input.hotel(), input.id(), state.status(), input.status()));
        return receipt;
    }
}
