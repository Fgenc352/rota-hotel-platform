package com.hotelbooking.pms.domain;

import java.util.UUID;

public record PhysicalRoom(UUID id, UUID roomTypeId, String number, Housekeeping housekeeping) {
}
