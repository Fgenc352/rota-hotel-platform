package com.hotelbooking.events.application.model;

import java.util.UUID;

/** Metadata only: guest contact details never enter the Kafka topic. */
public record BrokerReservationEvent(
    UUID eventId, UUID hotelId, UUID reservationId, int aggregateVersion, String type, String traceId
    ) {
    public BrokerReservationEvent {
        if (eventId == null || hotelId == null || reservationId == null || aggregateVersion < 1
            || type == null || type.isBlank() || type.length() > 60
            || traceId == null || traceId.isBlank() || traceId.length() > 64) {
            throw new IllegalArgumentException("Invalid reservation event envelope");
        }
    }
}
