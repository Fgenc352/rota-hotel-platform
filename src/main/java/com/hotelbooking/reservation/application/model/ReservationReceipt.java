package com.hotelbooking.reservation.application.model;

import java.math.BigDecimal;
import java.util.UUID;

public record ReservationReceipt(
    UUID id,
    String code,
    String status,
    BigDecimal total,
    String currency
    ) {
}
