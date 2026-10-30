package com.hotelbooking.guest.application.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record GuestSummary(
    UUID id,
    UUID hotelId,
    String firstName,
    String lastName,
    String email,
    String phone,
    boolean marketingAllowed,
    Instant consentAt,
    long stays,
    BigDecimal spend,
    long activeReservations,
    BigDecimal bookedValue
    ) {
}
