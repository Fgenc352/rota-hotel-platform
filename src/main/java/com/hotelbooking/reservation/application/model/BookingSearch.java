package com.hotelbooking.reservation.application.model;

import com.hotelbooking.reservation.domain.ReservationStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;

public record BookingSearch(
    @Size(max = 100) String code,
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @Size(max = 254) String email,
    @Size(max = 30) String phone,
    UUID roomTypeId,
    ReservationStatus status,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInTo,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutTo,
    @Min(1) Integer adults,
    @Min(0) Integer children,
    @DecimalMin("0") BigDecimal minTotal,
    @DecimalMin("0") BigDecimal maxTotal
    ) {
    public static BookingSearch empty() {
        return new BookingSearch(null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null);
    }

    @AssertTrue(message = "Tarih ve tutar aralıklarının başlangıcı bitişinden büyük olamaz.")
    public boolean isRangeValid() {
        return (checkInFrom == null || checkInTo == null || !checkInFrom.isAfter(checkInTo)) && (checkOutFrom == null || checkOutTo == null || !checkOutFrom.isAfter(checkOutTo)) && (minTotal == null || maxTotal == null || minTotal.compareTo(maxTotal) <= 0);
    }
}
