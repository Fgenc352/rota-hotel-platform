package com.hotelbooking.pms.application.model;

import java.util.List;
import java.util.UUID;

public record ReservationServices(UUID reservationId, String code, String status, List<ServiceTaskView> services,
    List<ServiceTaskAudit> history) {
}
