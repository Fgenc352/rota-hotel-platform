package com.hotelbooking.reservation.event;

import com.hotelbooking.events.application.port.EventOutbox;
import com.hotelbooking.reservation.application.port.ReservationSnapshots;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/** Writes the snapshot and deliveries inside the reservation command's transaction. */
@Component
@RequiredArgsConstructor
public class ReservationEventPublisher {
    private final ReservationSnapshots snapshots;
    private final EventOutbox outbox;

    public void publish(UUID hotel, UUID id, String type) {
        var snapshot = snapshots.get(hotel, id);
        outbox.append(UUID.randomUUID(), hotel, id, type, snapshot,
            Optional.ofNullable(MDC.get("traceId")).orElseGet(() -> UUID.randomUUID().toString()));
    }
}
