package com.hotelbooking.pms.application.model;

import com.hotelbooking.pms.domain.DailyServiceStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record DailyServiceTask(UUID reservationId, String code, LocalDate date, DailyServiceStatus status,
    LocalTime time, String pickup, String destination, String plate, String notes, int passengers, String actor,
    Instant updatedAt) {
}
