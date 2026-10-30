package com.hotelbooking.inventory.domain;

import com.hotelbooking.shared.domain.Stay;
import com.hotelbooking.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

public record Room(
    UUID id,
    UUID hotelId,
    String name,
    int capacity,
    int rooms,
    BigDecimal nightlyPrice,
    BigDecimal weekendMultiplier,
    int minNights
    ) {
    public void validateStay(Stay stay) {
        if (stay.people() > capacity) {
            throw BusinessException.invalid("Oda kapasitesi aşıldı.");
        }
        if (stay.nights() < minNights) {
            throw BusinessException.invalid("Minimum konaklama süresi sağlanmadı.");
        }
    }

    public BigDecimal total(Stay stay) {
        return stay.dates().stream().map(this::price).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal price(LocalDate date) {
        boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        return nightlyPrice.multiply(weekend ? weekendMultiplier : BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP);
    }
}
