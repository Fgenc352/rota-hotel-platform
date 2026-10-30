package com.hotelbooking.unit;

import com.hotelbooking.hotel.application.port.HotelCatalog;
import com.hotelbooking.inventory.application.port.InventoryCommands;
import com.hotelbooking.pms.application.command.UpdateDailyServiceHandler;
import com.hotelbooking.pms.application.model.DailyServiceTask;
import com.hotelbooking.pms.application.model.RoomView;
import com.hotelbooking.pms.application.port.DailyServiceTasks;
import com.hotelbooking.pms.application.port.RoomBoardQueries;
import com.hotelbooking.pms.application.query.GetDailyOperationsHandler;
import com.hotelbooking.pms.domain.DailyServiceRules;
import com.hotelbooking.pms.domain.DailyServiceStatus;
import com.hotelbooking.pms.domain.Housekeeping;
import com.hotelbooking.reservation.application.model.BookingSummary;
import com.hotelbooking.reservation.application.port.BookingQueries;
import com.hotelbooking.reservation.application.port.Reservations;
import com.hotelbooking.reservation.domain.*;
import com.hotelbooking.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("unit")
class PmsDailyOperationsTest {
    private final UUID hotel = UUID.randomUUID(), id = UUID.randomUUID(), type = UUID.randomUUID();
    private final LocalDate in = LocalDate.now(java.time.ZoneId.of("Europe/Istanbul")).minusDays(2), out = LocalDate.now(java.time.ZoneId.of("Europe/Istanbul")).plusDays(1);
    private final BookingQueries bookings = mock(BookingQueries.class);
    private final DailyServiceTasks tasks = mock(DailyServiceTasks.class);
    private final RoomBoardQueries rooms = mock(RoomBoardQueries.class);
    private final Reservations reservations = mock(Reservations.class);
    private final HotelCatalog hotels = mock(HotelCatalog.class);
    private final InventoryCommands inventory = mock(InventoryCommands.class);

    private BookingSummary booking(String status, List<String> codes) {
        return new BookingSummary(id, "RT-DAILY", "Deniz", "Test", in, out, status, new BigDecimal("10000"),
            "Standart", null, type,
            "daily@example.com", "+905551234567", 2, 1, "TRY", new BigDecimal("9000"), Instant.now(), null,
            codes.stream().map(code -> new ServiceLine(code, code, ServiceUnit.PER_PERSON_PER_NIGHT, BigDecimal.TEN,
            9, new BigDecimal("90"), false)).toList());
    }
    private GetDailyOperationsHandler query(BookingSummary booking) {
        when(hotels.timezone(hotel)).thenReturn("Europe/Istanbul");
        when(bookings.daily(eq(hotel), any())).thenReturn(List.of(booking));
        when(tasks.tasks(eq(hotel), any(), any())).thenReturn(List.of());
        when(rooms.dailyAssignments(eq(hotel), any())).thenReturn(List.of());
        when(rooms.rooms(hotel)).thenReturn(List.of(new RoomView(UUID.randomUUID(), type, "Standart", "101",
            Housekeeping.CLEAN, null, null, 0)));
        return new GetDailyOperationsHandler(bookings, tasks, rooms, hotels);
    }
    private UpdateDailyServiceHandler command(String status, List<String> codes) {
        when(hotels.timezone(hotel)).thenReturn("Europe/Istanbul");
        when(reservations.roomId(hotel, id)).thenReturn(type);
        when(reservations.lock(hotel, id)).thenReturn(new ReservationState("RT-DAILY", ReservationStatus.valueOf(status),
            new BigDecimal("10000"), in, out));
        when(bookings.detail(hotel, id)).thenReturn(booking(status, codes));
        when(tasks.tasks(eq(hotel), any(), any())).thenReturn(List.of());
        return new UpdateDailyServiceHandler(reservations, inventory, bookings, tasks, hotels);
    }
    private UpdateDailyServiceHandler.Input input(String code, LocalDate date, DailyServiceStatus status) {
        return new UpdateDailyServiceHandler.Input(hotel, id, code, date, status, LocalTime.of(9, 0), "Airport",
            "Hotel", "34 TEST 01", "", 3, "staff");
    }

    @Test
    void breakfastUsesMorningsIncludingCheckoutAndMealsUseCheckoutExclusiveDays() {
        assertThat(DailyServiceRules.due("BREAKFAST", in, in, out)).isFalse();
        assertThat(DailyServiceRules.due("BREAKFAST", out, in, out)).isTrue();
        assertThat(DailyServiceRules.due("LUNCH", in, in, out)).isTrue();
        assertThat(DailyServiceRules.due("DINNER", out, in, out)).isFalse();
        assertThat(DailyServiceRules.due("VALET", out, in, out)).isTrue();
        assertThat(DailyServiceRules.due("WIFI", in, in, out)).isFalse();
    }

    @Test
    void completingBreakfastOnOneDayDoesNotCompleteTheNextDayOrOtherMeals() {
        var query = query(booking("CHECKED_IN", List.of("BREAKFAST", "LUNCH", "DINNER")));
        var yesterday = in.plusDays(1);
        when(tasks.tasks(hotel, List.of(id), yesterday)).thenReturn(List.of(new DailyServiceTask(id, "BREAKFAST",
            yesterday, DailyServiceStatus.SERVED,
            null, "", "", "", "", 3, "staff", Instant.now())));
        assertThat(query.handle(hotel, yesterday).services()).filteredOn(s -> s.task().code().equals("BREAKFAST"))
            .allMatch(s -> s.task().status() == DailyServiceStatus.SERVED && s.quantity() == 3);
        assertThat(query.handle(hotel, yesterday.plusDays(1)).services()).allMatch(s -> s.task().status() == DailyServiceStatus.PENDING);
        assertThat(query.handle(hotel, out).services()).extracting(s -> s.task().code()).containsExactly("BREAKFAST");
    }

    @Test
    void futureTransferCanBePlannedBeforeGuestChecksInAndReschedulingRemovesDefaultEntry() {
        var command = command("CONFIRMED", List.of("TRANSFER"));
        command.handle(input("TRANSFER", out, DailyServiceStatus.PLANNED));
        verify(tasks).save(eq(hotel), argThat(t -> t.status() == DailyServiceStatus.PLANNED && t.date().equals(out)));
        var query = query(booking("CONFIRMED", List.of("TRANSFER")));
        var stored = new DailyServiceTask(id, "TRANSFER", out, DailyServiceStatus.PLANNED, LocalTime.NOON,
            "Airport", "Hotel", "", "", 3, "staff", Instant.now());
        when(tasks.tasks(eq(hotel), any(), any())).thenReturn(List.of(stored));
        assertThat(query.handle(hotel, in).services()).isEmpty();
        assertThat(query.handle(hotel, in).upcomingTransfers()).hasSize(1);
        assertThat(query.handle(hotel, out).services()).extracting(s -> s.task().date()).containsExactly(out);
    }

    @Test
    void transferNeedsScheduleAndCannotSkipOperationalSteps() {
        var handler = command("CONFIRMED", List.of("TRANSFER"));
        var missing = new UpdateDailyServiceHandler.Input(hotel, id, "TRANSFER", in, DailyServiceStatus.PLANNED,
            null, "", "", "", "", 3, "staff");
        assertThatThrownBy(() -> handler.handle(missing)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> handler.handle(input("TRANSFER", in, DailyServiceStatus.COMPLETED))).isInstanceOf(BusinessException.class);
        verify(tasks, never()).save(any(), any());
    }

    @Test
    void cancelledReservationsAndUnselectedServicesCannotBeServed() {
        var cancelled = command("CANCELLED", List.of("BREAKFAST"));
        assertThatThrownBy(() -> cancelled.handle(input("BREAKFAST", LocalDate.now(java.time.ZoneId.of("Europe/Istanbul")),
            DailyServiceStatus.SERVED))).isInstanceOf(BusinessException.class);
        var valid = command("CHECKED_IN", List.of("BREAKFAST"));
        assertThatThrownBy(() -> valid.handle(input("LUNCH", LocalDate.now(java.time.ZoneId.of("Europe/Istanbul")),
            DailyServiceStatus.SERVED))).isInstanceOf(BusinessException.class);
        verify(tasks, never()).save(any(), any());
    }

    @Test
    void mealsCannotBeServedBeforeArrivalOrOnFutureDaysAndDoNotChangePrice() {
        var confirmed = command("CONFIRMED", List.of("BREAKFAST"));
        assertThatThrownBy(() -> confirmed.handle(input("BREAKFAST", LocalDate.now(java.time.ZoneId.of("Europe/Istanbul")),
            DailyServiceStatus.SERVED))).isInstanceOf(BusinessException.class);
        var checkedIn = command("CHECKED_IN", List.of("BREAKFAST"));
        assertThatThrownBy(() -> checkedIn.handle(input("BREAKFAST", out, DailyServiceStatus.SERVED))).isInstanceOf(BusinessException.class);
        checkedIn.handle(input("BREAKFAST", LocalDate.now(java.time.ZoneId.of("Europe/Istanbul")), DailyServiceStatus.SERVED));
        verify(tasks).save(eq(hotel), argThat(t -> t.code().equals("BREAKFAST") && t.status() == DailyServiceStatus.SERVED));
        verify(reservations, never()).changeStatus(any(), any(), any());
        verify(inventory, never()).release(any(), any(), any(), any());
    }

    @Test
    void valetRequiresPlateAndTerminalStatesCannotRegress() {
        var handler = command("CHECKED_IN", List.of("VALET"));
        var missing = new UpdateDailyServiceHandler.Input(hotel, id, "VALET", in, DailyServiceStatus.RECEIVED,
            null, "", "", "", "", 0, "staff");
        assertThatThrownBy(() -> handler.handle(missing)).isInstanceOf(BusinessException.class);
        handler.handle(input("VALET", in, DailyServiceStatus.RECEIVED));
        assertThatThrownBy(() -> DailyServiceRules.transition("VALET", DailyServiceStatus.DELIVERED, DailyServiceStatus.PARKED)).isInstanceOf(BusinessException.class);
    }

    @Test
    void earlyCheckoutStopsFutureMealEntitlementsWithoutChangingOriginalBookingDates() {
        var original = booking("CHECKED_OUT", List.of("BREAKFAST", "LUNCH", "DINNER"));
        var early = new BookingSummary(original.id(), original.code(), original.firstName(), original.lastName(),
            original.checkIn(), original.checkOut(),
            original.status(), original.total(), original.roomName(), original.guestId(), original.roomTypeId(),
            original.email(), original.phone(),
            original.adults(), original.children(), original.currency(), original.roomTotal(), original.createdAt(),
            Instant.now(), original.services());
        var query = query(early);
        assertThat(query.handle(hotel, LocalDate.now(java.time.ZoneId.of("Europe/Istanbul"))).services()).extracting(s -> s.task().code()).containsExactly("BREAKFAST");
        assertThat(query.handle(hotel, out).services()).isEmpty();
    }

    @Test
    void completedDailyTaskRetryIsIdempotentAndCannotChangeItsNotes() {
        var handler = command("CHECKED_IN", List.of("BREAKFAST"));
        var previous = new DailyServiceTask(id, "BREAKFAST", in.plusDays(1), DailyServiceStatus.SERVED, LocalTime.of(9,
            0), "Airport", "Hotel", "34 TEST 01", "", 3, "staff", Instant.now());
        when(tasks.tasks(hotel, List.of(id), in.plusDays(1))).thenReturn(List.of(previous));
        handler.handle(input("BREAKFAST", in.plusDays(1), DailyServiceStatus.SERVED));
        verify(tasks, never()).save(any(), any());
        var changed = new UpdateDailyServiceHandler.Input(hotel, id, "BREAKFAST", in.plusDays(1), DailyServiceStatus.SERVED,
            LocalTime.NOON, "", "", "", "Changed", 3, "staff");
        assertThatThrownBy(() -> handler.handle(changed)).isInstanceOf(BusinessException.class);
    }
    @Test
    void checkoutAndDefaultDayUseHotelTimezoneAndExtremeDatesNeverReachDatabase() {
        Instant lateUtc = Instant.parse("2026-10-28T22:30:00Z");
        LocalDate planned = LocalDate.of(2026, 10, 30);
        assertThat(DailyServiceRules.end(planned, lateUtc, java.time.ZoneId.of("Europe/Istanbul"))).isEqualTo(LocalDate.of(2026,
            10, 29));
        assertThat(DailyServiceRules.end(planned, lateUtc, java.time.ZoneId.of("UTC"))).isEqualTo(LocalDate.of(2026,
            10, 28));
        var handler = query(booking("CHECKED_IN", List.of("BREAKFAST")));
        assertThat(handler.handle(hotel, null).date()).isEqualTo(LocalDate.now(java.time.ZoneId.of("Europe/Istanbul")));
        clearInvocations(bookings);
        assertThatThrownBy(() -> handler.handle(hotel, LocalDate.MAX)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(bookings);
    }

    @Test
    void earlyCheckoutKeepsAlreadyServedMealsVisibleAndTheirRetriesIdempotent() {
        var handler = command("CHECKED_OUT", List.of("LUNCH"));
        var original = booking("CHECKED_OUT", List.of("LUNCH"));
        var early = new BookingSummary(original.id(), original.code(), original.firstName(), original.lastName(),
            original.checkIn(), original.checkOut(),
            original.status(), original.total(), original.roomName(), original.guestId(), original.roomTypeId(),
            original.email(), original.phone(),
            original.adults(), original.children(), original.currency(), original.roomTotal(), original.createdAt(),
            Instant.now(), original.services());
        when(bookings.detail(hotel, id)).thenReturn(early);
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Istanbul"));
        var served = new DailyServiceTask(id, "LUNCH", today, DailyServiceStatus.SERVED, LocalTime.of(9, 0),
            "Airport", "Hotel", "34 TEST 01", "", 3, "staff", Instant.now());
        var query = query(early);
        when(tasks.tasks(hotel, List.of(id), today)).thenReturn(List.of(served));
        assertThat(query.handle(hotel, today).services()).hasSize(1)
            .allMatch(item -> item.task().status() == DailyServiceStatus.SERVED);
        handler.handle(input("LUNCH", today, DailyServiceStatus.SERVED));
        verify(tasks, never()).save(any(), any());
    }

    @Test
    void unsupportedCalendarDatesNeverReachDailyCommandPersistence() {
        var handler = command("CHECKED_IN", List.of("BREAKFAST"));
        assertThatThrownBy(() -> handler.handle(input("BREAKFAST", LocalDate.MAX, DailyServiceStatus.SERVED)))
            .isInstanceOf(BusinessException.class);
        verifyNoInteractions(reservations, inventory, tasks);
    }

}
