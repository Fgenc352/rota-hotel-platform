package com.hotelbooking.pms.application.model;

import java.util.List;

public record RoomAuditPage(List<RoomAudit> items, long total, int page, int size) {
}
