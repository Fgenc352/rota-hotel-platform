package com.hotelbooking.pms.api.dto;

import com.hotelbooking.pms.domain.Housekeeping;
import jakarta.validation.constraints.NotNull;

public record HousekeepingRequest(@NotNull Housekeeping state) {
}
