package com.hotelbooking.pms.application.query;

import com.hotelbooking.pms.application.model.DailyServiceAudit;
import com.hotelbooking.pms.application.port.DailyServiceTasks;
import com.hotelbooking.reservation.application.port.BookingQueries;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetDailyServiceHistoryHandler {
    private final BookingQueries bookings;
    private final DailyServiceTasks tasks;

    public List<DailyServiceAudit> handle(UUID hotel, UUID reservation, String code, LocalDate date) {
        com.hotelbooking.pms.domain.DailyServiceRules.operationDate(date);
        bookings.detail(hotel, reservation);
        return tasks.history(hotel, reservation, code, date);
    }
}
