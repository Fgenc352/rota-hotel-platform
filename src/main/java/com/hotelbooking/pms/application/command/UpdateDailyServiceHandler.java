package com.hotelbooking.pms.application.command;

import com.hotelbooking.hotel.application.port.HotelCatalog;
import com.hotelbooking.inventory.application.port.InventoryCommands;
import java.time.ZoneId;
import com.hotelbooking.pms.application.model.DailyServiceTask;
import com.hotelbooking.pms.application.port.DailyServiceTasks;
import com.hotelbooking.pms.domain.DailyServiceRules;
import com.hotelbooking.pms.domain.DailyServiceStatus;
import com.hotelbooking.reservation.application.port.BookingQueries;
import com.hotelbooking.reservation.application.port.Reservations;
import com.hotelbooking.shared.exception.BusinessException;
import com.hotelbooking.shared.logging.TransactionLog;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UpdateDailyServiceHandler {
    public record Input(UUID hotel, UUID reservation, String code, LocalDate date, DailyServiceStatus status,
        LocalTime time, String pickup, String destination, String plate, String notes, int passengers, String actor) {
    }
    private final Reservations reservations;
    private final InventoryCommands inventory;
    private final BookingQueries bookings;
    private final DailyServiceTasks tasks;
    private final HotelCatalog hotels;

    public void handle(Input input) {
        DailyServiceRules.operationDate(input.date());
        var type = reservations.roomId(input.hotel(), input.reservation());
        inventory.room(input.hotel(), type);
        var state = reservations.lock(input.hotel(), input.reservation());
        if (!List.of("CONFIRMED", "CHECKED_IN", "CHECKED_OUT").contains(state.status().name())) {
            throw BusinessException.conflict("SERVICE_RESERVATION_CLOSED", "Bu rezervasyonda hizmet işlemi yapılamaz.");
        }
        var timezone = ZoneId.of(hotels.timezone(input.hotel()));
        var booking = bookings.detail(input.hotel(), input.reservation());
        if (booking.services().stream().noneMatch(line -> line.code().equals(input.code())) || DailyServiceRules.workflow(input.code()).isEmpty()) {
            throw BusinessException.invalid("Hizmet rezervasyonun paketinde bulunmuyor.");
        }
        var previous = tasks.tasks(input.hotel(), List.of(input.reservation()), input.date()).stream()
            .filter(t -> t.code().equals(input.code()) && (input.code().equals("TRANSFER") || t.date().equals(input.date()))).findFirst();
        var from = previous.map(DailyServiceTask::status).orElse(DailyServiceStatus.PENDING);
        // Early checkout ends unstarted entitlements, but must preserve a service already performed/started.
        var serviceEnd = input.code().equals("TRANSFER") || from != DailyServiceStatus.PENDING ? booking.checkOut()
            : DailyServiceRules.end(booking.checkOut(), booking.checkedOutAt(), timezone);
        DailyServiceRules.date(input.code(), input.date(), booking.checkIn(), serviceEnd);
        DailyServiceRules.transition(input.code(), from, input.status());
        if (input.status() != DailyServiceStatus.PENDING && input.status() != DailyServiceStatus.PLANNED) {
            if (input.date().isAfter(LocalDate.now(timezone))) throw BusinessException.invalid("Gelecekteki hizmet verildi olarak işaretlenemez.");
            if (!input.code().equals("TRANSFER") && !List.of("CHECKED_IN", "CHECKED_OUT").contains(booking.status())) {
                throw BusinessException.conflict("GUEST_NOT_ARRIVED", "Misafir giriş yapmadan yemek/vale teslimi kaydedilemez.");
            }
        }
        if (input.code().equals("TRANSFER")) {
            if (input.passengers()<1 || input.passengers()>booking.adults()+booking.children()) throw BusinessException.invalid("Yolcu sayısı rezervasyondaki kişi sayısını aşamaz.");
            if (input.status() != DailyServiceStatus.PENDING && (input.time() == null || blank(input.pickup()) || blank(input.destination()))) {
                throw BusinessException.invalid("Transfer planı için saat, alış ve varış noktası gereklidir.");
            }
            if (previous.isPresent() && from != DailyServiceStatus.PENDING && from != DailyServiceStatus.PLANNED && !previous.get().date().equals(input.date())) {
                throw BusinessException.conflict("TRANSFER_STARTED", "Başlamış transfer başka güne taşınamaz.");
            }
        }
        if (input.code().equals("VALET") && input.status() != DailyServiceStatus.PENDING && blank(input.plate())) throw BusinessException.invalid("Vale işlemi için plaka gereklidir.");
        var next = new DailyServiceTask(input.reservation(), input.code(), input.date(), input.status(), input.time(),
            clean(input.pickup()), clean(input.destination()), clean(input.plate()), clean(input.notes()), input.passengers(),
            input.actor(), Instant.now());
        if (previous.isPresent() && same(previous.get(), next)) return;
        if (previous.isPresent() && List.of(DailyServiceStatus.SERVED, DailyServiceStatus.COMPLETED, DailyServiceStatus.DELIVERED).contains(from)) {
            throw BusinessException.conflict("SERVICE_COMPLETED", "Tamamlanan günlük hizmet değiştirilemez.");
        }
        tasks.save(input.hotel(), next);
        TransactionLog.afterCommit(() -> log.info("event=pms.daily_service_changed hotelId={} reservationId={} service={} date={} status={}",
            input.hotel(), input.reservation(), input.code(), input.date(), input.status()));
    }
    private boolean same(DailyServiceTask a, DailyServiceTask b) {
        return a.date().equals(b.date()) && a.status() == b.status() && java.util.Objects.equals(a.time(),
            b.time())
        && java.util.Objects.equals(a.pickup(), b.pickup()) && java.util.Objects.equals(a.destination(), b.destination())
        && java.util.Objects.equals(a.plate(), b.plate()) && java.util.Objects.equals(a.notes(), b.notes()) && a.passengers() == b.passengers();
    }
    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
