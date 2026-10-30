package com.hotelbooking.shared.domain;

import com.hotelbooking.shared.exception.BusinessException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public record Stay(LocalDate checkIn, LocalDate checkOut, int adults, int children) {
    public void validate(LocalDate today) {
        if (checkIn == null || checkOut == null) {
            throw new IllegalArgumentException("Giriş ve çıkış tarihleri zorunludur.");
        }
        if (checkIn.isBefore(today)) {
            throw new IllegalArgumentException("Geçmiş tarihte rezervasyon yapılamaz.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Çıkış tarihi girişten sonra olmalıdır.");
        }
        if (nights() > 30 || checkOut.isAfter(today.plusYears(1))) {
            throw new IllegalArgumentException("En fazla 30 gece ve bir yıl ileri tarih seçilebilir.");
        }
        if (adults < 1 || adults > 10 || children < 0 || children > 10) {
            throw new IllegalArgumentException("Kişi sayısı geçersiz.");
        }
    }

    public void validateForBooking(LocalDate today) {
        try {
            validate(today);
        } catch (IllegalArgumentException e) {
            throw BusinessException.invalid(e.getMessage());
        }
    }

    public long nights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public int people() {
        return adults + children;
    }

    public List<LocalDate> dates() {
        return checkIn.datesUntil(checkOut).toList();
    }
}
