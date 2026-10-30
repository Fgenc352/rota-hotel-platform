package com.hotelbooking.pms.application.model;

import java.time.LocalDate;
import java.util.UUID;

public record DailyStay(UUID reservationId, String code, String guestName, String phone, String roomNumber,
    String roomType, int people, LocalDate checkIn, LocalDate checkOut, String status, boolean arrival, boolean departure,
    java.time.Instant checkedOutAt) {
}
