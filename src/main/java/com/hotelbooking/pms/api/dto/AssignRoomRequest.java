package com.hotelbooking.pms.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignRoomRequest(@NotNull UUID roomId) {
}
