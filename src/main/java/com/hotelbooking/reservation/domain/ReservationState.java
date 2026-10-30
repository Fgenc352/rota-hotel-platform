package com.hotelbooking.reservation.domain;

import com.hotelbooking.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationState(
    String code,
    ReservationStatus status,
    BigDecimal total,
    LocalDate checkIn,
    LocalDate checkOut
    ) {
    public void validateTransition(ReservationStatus next, LocalDate today) {
        if (status == next) {
            return;
        }
        if (!status.canTransitionTo(next)) {
            throw BusinessException.conflict("INVALID_TRANSITION", "Bu rezervasyon durumu geçişine izin verilmiyor.");
        }
        if (today.isBefore(checkIn)) {
            switch (next) {
                case CHECKED_IN -> throw BusinessException.invalid("Giriş tarihinden önce check-in yapılamaz.");
                case CHECKED_OUT -> throw BusinessException.invalid("Konaklama başlamadan check-out yapılamaz.");
                case NO_SHOW -> throw BusinessException.invalid("Giriş tarihinden önce no-show yapılamaz.");
                default -> {
                }
            }
        }
    }
}
