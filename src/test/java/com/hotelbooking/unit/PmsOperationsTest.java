package com.hotelbooking.unit;

import com.hotelbooking.inventory.application.port.InventoryCommands;
import com.hotelbooking.pms.application.command.*;
import com.hotelbooking.pms.application.port.RoomOperations;
import com.hotelbooking.pms.application.port.ServiceTasks;
import com.hotelbooking.pms.domain.*;
import com.hotelbooking.reservation.application.model.BookingSummary;
import com.hotelbooking.reservation.application.port.Reservations;
import com.hotelbooking.reservation.application.query.GetBookingHandler;
import com.hotelbooking.reservation.domain.*;
import com.hotelbooking.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("unit")
class PmsOperationsTest {
    private final UUID hotel = UUID.randomUUID(), reservation = UUID.randomUUID(), room = UUID.randomUUID(),
    type = UUID.randomUUID();
    private final LocalDate in = LocalDate.of(2026, 11, 7), out = in.plusDays(2);

    private RoomAssignment assigned(String status) {
        return new RoomAssignment(reservation, room, "RT-test", in, out, status);
    }
    private PhysicalRoom physical(Housekeeping state) {
        return new PhysicalRoom(room, type, "101", state);
    }

    @Test
    void wrongTypeAndClosedReservationCannotBeAssigned() {
        assertThatThrownBy(() -> RoomRules.assignable(physical(Housekeeping.CLEAN), UUID.randomUUID(), "CONFIRMED"))
            .isInstanceOf(BusinessException.class).extracting(error -> ((BusinessException)error).code()).isEqualTo("ROOM_TYPE_MISMATCH");
        assertThatThrownBy(() -> RoomRules.assignable(physical(Housekeeping.CLEAN), type, "CHECKED_OUT")).isInstanceOf(BusinessException.class);
    }

    @Test
    void conflictingRoomDoesNotChangeTheAssignment() {
        var rooms = mock(RoomOperations.class);
        var reservations = mock(Reservations.class);
        var inventory = mock(InventoryCommands.class);
        when(reservations.roomId(hotel, reservation)).thenReturn(type);
        when(reservations.lock(hotel, reservation)).thenReturn(new ReservationState("RT-test", ReservationStatus.CONFIRMED,
            BigDecimal.TEN, in, out));
        when(rooms.assignment(hotel, reservation)).thenReturn(Optional.empty());
        when(rooms.lockRoom(hotel, room)).thenReturn(physical(Housekeeping.CLEAN));
        when(rooms.conflicts(hotel, room, reservation, in, out, false)).thenReturn(true);
        assertThatThrownBy(() -> new AssignRoomHandler(reservations, inventory, rooms).handle(new AssignRoomHandler.Input(hotel,
            reservation, room, "staff")))
            .isInstanceOf(BusinessException.class);
        verify(rooms, never()).assign(any(), any());
    }

    @Test
    void checkoutUpdatesAssignmentAndMarksPhysicalRoomDirty() {
        var rooms = mock(RoomOperations.class);
        when(rooms.assignment(hotel, reservation)).thenReturn(Optional.of(assigned("CHECKED_IN")));
        when(rooms.lockRoom(hotel, room)).thenReturn(physical(Housekeeping.CLEAN));
        new ApplyStayTransitionHandler(rooms).handle(new ApplyStayTransitionHandler.Input(hotel, reservation,
            "CHECKED_OUT", "staff"));
        verify(rooms).transition(hotel, reservation, "CHECKED_OUT");
        verify(rooms).housekeeping(hotel, room, Housekeeping.DIRTY);
        verify(rooms).audit(eq(hotel), eq(room), eq(reservation), eq("STAY_STATUS_CHANGED"), eq("staff"), any(),
            any());
    }

    @Test
    void dirtyAndOccupiedRoomsCannotCheckInButLegacyUnassignedStaysRemainSupported() {
        var rooms = mock(RoomOperations.class);
        when(rooms.assignment(hotel, reservation)).thenReturn(Optional.of(assigned("CONFIRMED")));
        when(rooms.lockRoom(hotel, room)).thenReturn(physical(Housekeeping.DIRTY));
        var handler = new ApplyStayTransitionHandler(rooms);
        assertThatThrownBy(() -> handler.handle(new ApplyStayTransitionHandler.Input(hotel, reservation, "CHECKED_IN",
            "staff")))
            .isInstanceOf(BusinessException.class);
        when(rooms.lockRoom(hotel, room)).thenReturn(physical(Housekeeping.CLEAN));
        when(rooms.conflicts(hotel, room, reservation, in, out, true)).thenReturn(true);
        assertThatThrownBy(() -> handler.handle(new ApplyStayTransitionHandler.Input(hotel, reservation, "CHECKED_IN",
            "staff")))
            .isInstanceOf(BusinessException.class);
        when(rooms.assignment(hotel, reservation)).thenReturn(Optional.empty());
        assertThatCode(() -> handler.handle(new ApplyStayTransitionHandler.Input(hotel, reservation, "CHECKED_IN",
            "staff"))).doesNotThrowAnyException();
        verify(rooms, never()).transition(any(), any(), any());
    }

    @Test
    void unchangedHousekeepingDoesNotCreateFakeAuditRecords() {
        var rooms = mock(RoomOperations.class);
        when(rooms.lockRoom(hotel, room)).thenReturn(physical(Housekeeping.CLEAN));
        new ChangeHousekeepingHandler(rooms).handle(new ChangeHousekeepingHandler.Input(hotel, room, Housekeeping.CLEAN,
            "staff"));
        verify(rooms, never()).housekeeping(any(), any(), any());
        verify(rooms, never()).audit(any(), any(), any(), any(), any(), any(), any());
    }

    private BookingSummary booking() {
        var valet = new ServiceLine("VALET", "Vale", ServiceUnit.PER_NIGHT, new BigDecimal("300.00"), 2, new BigDecimal("600.00"),
            false);
        return new BookingSummary(reservation, "RT-test", "Test", "Guest", in, out, "CONFIRMED", new BigDecimal("7000.00"),
            "Room", null, type,
            "test@example.com", "+905551111111", 2, 0, "TRY", new BigDecimal("6400.00"), Instant.now(), null, List.of(valet));
    }

    @Test
    void packageInformationRetainsOriginalPricesAndArchivedHistoryWithoutWriting() {
        var bookings = mock(GetBookingHandler.class);
        var tasks = mock(ServiceTasks.class);
        when(bookings.handle(hotel, reservation)).thenReturn(booking());
        when(tasks.states(hotel, reservation)).thenReturn(List.of());
        when(tasks.history(hotel, reservation)).thenReturn(List.of());
        var result = new com.hotelbooking.pms.application.query.GetReservationServicesHandler(bookings, tasks).handle(hotel,
            reservation);
        assertThat(result.services()).hasSize(1);
        assertThat(result.services().getFirst().line().total()).isEqualByComparingTo("600.00");
        assertThat(result.services().getFirst().line().code()).isEqualTo("VALET");
        assertThat(result.history()).isEmpty();
        verify(tasks).states(hotel, reservation);
        verify(tasks).history(hotel, reservation);
        verifyNoMoreInteractions(tasks);
    }
}
