package com.hotelbooking.pms.application.model;

import java.time.LocalDate;
import java.util.List;

public record DailyOperations(LocalDate date, List<DailyStay> stays, List<DailyServiceItem> services, List<DailyServiceItem> upcomingTransfers,
    int totalRooms, int occupiedNow, int emptyNow, int plannedOccupied, int arrivals, int departures) {
}
