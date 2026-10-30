package com.hotelbooking.pms.application.model;

import java.util.UUID;

public record DailyServiceItem(UUID reservationId, String bookingCode, String guestName, String phone, String roomNumber,
    String reservationStatus, String serviceName, int quantity, DailyServiceTask task) {
}
