package com.hotelbooking.pms.application.query;

import com.hotelbooking.hotel.application.port.HotelCatalog;
import com.hotelbooking.pms.application.model.*;
import java.time.ZoneId;
import com.hotelbooking.pms.application.port.DailyServiceTasks;
import com.hotelbooking.pms.application.port.RoomBoardQueries;
import com.hotelbooking.pms.domain.DailyServiceRules;
import com.hotelbooking.pms.domain.DailyServiceStatus;
import com.hotelbooking.reservation.application.port.BookingQueries;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetDailyOperationsHandler {
    private final BookingQueries bookings;
    private final DailyServiceTasks tasks;
    private final RoomBoardQueries rooms;
    private final HotelCatalog hotels;

    public DailyOperations handle(UUID hotel, LocalDate requestedDate) {
        var timezone = ZoneId.of(hotels.timezone(hotel));
        LocalDate date = requestedDate == null ? LocalDate.now(timezone) : requestedDate;
        DailyServiceRules.operationDate(date);
        var source = bookings.daily(hotel, date);
        var stored = tasks.tasks(hotel, source.stream().map(b -> b.id()).toList(), date);
        var taskIndex = stored.stream().collect(Collectors.toMap(t -> t.reservationId()+":"+t.code(), t -> t));
        var physical = rooms.rooms(hotel);
        var assignments = rooms.dailyAssignments(hotel, date);
        var numbers = physical.stream().collect(Collectors.toMap(r -> r.id(), r -> r.number()));
        var assigned = assignments.stream().collect(Collectors.toMap(a -> a.reservationId(), a -> numbers.getOrDefault(a.roomId(),
            "Atanmamış")));
        var stays = new ArrayList<DailyStay>();
        var services = new ArrayList<DailyServiceItem>();
        var upcomingTransfers = new ArrayList<DailyServiceItem>();
        for (var booking : source) {
            LocalDate serviceEnd = DailyServiceRules.end(booking.checkOut(), booking.checkedOutAt(), timezone);
            boolean planned = !date.isBefore(booking.checkIn()) && date.isBefore(serviceEnd);
            boolean arrival = booking.checkIn().equals(date), departure = serviceEnd.equals(date);
            String room = assigned.getOrDefault(booking.id(), "Atanmamış");
            if (planned || arrival || departure || booking.status().equals("CHECKED_IN")) {
                stays.add(new DailyStay(booking.id(), booking.code(), booking.firstName()+" "+booking.lastName(),
                    booking.phone(), room,
                    booking.roomName(), booking.adults()+booking.children(), booking.checkIn(), booking.checkOut(),
                    booking.status(), arrival, departure, booking.checkedOutAt()));
            }
            for (var line : booking.services()) {
                var saved = java.util.Optional.ofNullable(taskIndex.get(booking.id()+":"+line.code()));
                boolean due = line.code().equals("TRANSFER") ? saved.map(t -> t.date().equals(date)).orElse(date.equals(booking.checkIn()))
                    : DailyServiceRules.due(line.code(), date, booking.checkIn(), serviceEnd)
                || saved.filter(task -> task.status() != DailyServiceStatus.PENDING).isPresent();
                if (DailyServiceRules.workflow(line.code()).isEmpty()) continue;
                LocalDate taskDate = line.code().equals("TRANSFER") ? saved.map(DailyServiceTask::date).orElse(booking.checkIn()) : date;
                boolean upcoming = line.code().equals("TRANSFER") && taskDate.isAfter(date) && !taskDate.isAfter(date.plusDays(7));
                if (!due && !upcoming) continue;
                var task = saved.orElse(new DailyServiceTask(booking.id(), line.code(), taskDate, DailyServiceStatus.PENDING,
                    null, "", "", "", "", booking.adults()+booking.children(), null, null));
                int quantity = line.code().equals("VALET") ? 1 : booking.adults()+booking.children();
                var item = new DailyServiceItem(booking.id(), booking.code(), booking.firstName()+" "+booking.lastName(),
                    booking.phone(), room,
                    booking.status(), line.name(), quantity, task);
                if (due) services.add(item);
                else upcomingTransfers.add(item);
            }
        }
        int occupied = (int) physical.stream().filter(r -> r.occupantId() != null).count();
        var overnightIds = source.stream().filter(b -> !date.isBefore(b.checkIn()) && date.isBefore(DailyServiceRules.end(b.checkOut(),
            b.checkedOutAt(), timezone)))
            .map(b -> b.id()).collect(Collectors.toSet());
        int planned = (int) assignments.stream().filter(a -> overnightIds.contains(a.reservationId())).map(a -> a.roomId()).distinct().count();
        return new DailyOperations(date, stays, services, upcomingTransfers, physical.size(), occupied, physical.size()-occupied,
            planned,
            (int) stays.stream().filter(DailyStay::arrival).count(), (int) stays.stream().filter(DailyStay::departure).count());
    }
}
