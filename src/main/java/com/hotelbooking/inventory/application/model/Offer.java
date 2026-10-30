package com.hotelbooking.inventory.application.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record Offer(
    UUID roomTypeId,
    String name,
    int capacity,
    int remaining,
    BigDecimal nightlyPrice,
    BigDecimal total,
    String currency,
    List<Night> nights,
    int minNights,
    BigDecimal baseNightlyPrice,
    BigDecimal weekendMultiplier
    ) {
}
