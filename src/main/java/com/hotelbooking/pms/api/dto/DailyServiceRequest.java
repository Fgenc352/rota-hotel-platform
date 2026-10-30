package com.hotelbooking.pms.api.dto;

import com.hotelbooking.pms.domain.DailyServiceStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record DailyServiceRequest(@NotNull LocalDate date, @NotNull DailyServiceStatus status, LocalTime time,
    @Size(max = 200) String pickup, @Size(max = 200) String destination, @Size(max = 30) String plate,
    @Size(max = 1000) String notes, @Min(0) @Max(100) int passengers) {
}
