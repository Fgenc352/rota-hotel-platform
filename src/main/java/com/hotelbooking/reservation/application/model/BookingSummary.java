package com.hotelbooking.reservation.application.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.hotelbooking.reservation.domain.ServiceLine;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record BookingSummary(
    UUID id,
    String code,
    String firstName,
    String lastName,
    LocalDate checkIn,
    LocalDate checkOut,
    String status,
    BigDecimal total,
    String roomName,
    UUID guestId,
    UUID roomTypeId,
    String email,
    String phone,
    int adults,
    int children,
    String currency,
    BigDecimal roomTotal,
    Instant createdAt,
    Instant checkedOutAt,
    List<ServiceLine> services
    ) {
}
