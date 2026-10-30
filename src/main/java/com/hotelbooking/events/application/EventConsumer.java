package com.hotelbooking.events.application;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import java.util.UUID;

public interface EventConsumer {
    String name();

    /** Local projections require atomic consumption; external services acknowledge through idempotency. */
    default boolean requiresLocalTransaction() {
        return true;
    }

    void accept(UUID event, String type, String traceId, ReservationSnapshot snapshot);
}
