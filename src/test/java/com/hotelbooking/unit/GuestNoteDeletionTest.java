package com.hotelbooking.unit;

import com.hotelbooking.guest.application.command.DeleteGuestNoteHandler;
import com.hotelbooking.guest.application.model.GuestNote;
import com.hotelbooking.guest.application.port.GuestCommands;
import com.hotelbooking.guest.domain.GuestNoteDeletionPolicy;
import com.hotelbooking.shared.exception.BusinessException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
class GuestNoteDeletionTest {
    static Stream<Arguments> boundaries() {
        return Stream.of(
            Arguments.of(Duration.ZERO, true),
            Arguments.of(Duration.ofHours(24).minusNanos(1), true),
            Arguments.of(Duration.ofHours(24), false),
            Arguments.of(Duration.ofHours(24).plusNanos(1), false),
            Arguments.of(Duration.ofNanos(-1), false));
    }

    @ParameterizedTest
    @MethodSource("boundaries")
    void deletionWindowIsExactlyTwentyFourHoursFromCreation(Duration age, boolean allowed) {
        var created = Instant.parse("2026-10-24T21:30:00Z");
        if (allowed) {
            assertThatCode(() -> GuestNoteDeletionPolicy.validate("staff", created, "staff", created.plus(age)))
                .doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> GuestNoteDeletionPolicy.validate("staff", created, "staff", created.plus(age)))
                .isInstanceOfSatisfying(BusinessException.class, error -> assertThat(error.code()).isEqualTo("NOTE_DELETE_WINDOW_CLOSED"));
        }
    }

    @Test
    void anotherStaffMemberCannotDeleteAFreshNote() {
        var created = Instant.parse("2026-10-30T10:00:00Z");
        assertThatThrownBy(() -> GuestNoteDeletionPolicy.validate("author", created, "other", created.plusSeconds(1)))
            .isInstanceOfSatisfying(BusinessException.class, error -> assertThat(error.kind()).isEqualTo(BusinessException.Kind.FORBIDDEN));
    }

    @Test
    void handlerLocksTheNoteWithHotelAndGuestScopeBeforeDeleting() {
        var repository = mock(GuestCommands.class);
        UUID hotel = UUID.randomUUID(), guest = UUID.randomUUID(), note = UUID.randomUUID();
        when(repository.lockNote(hotel, guest, note)).thenReturn(Optional.of(
            new GuestNote(note, "Note", "staff", Instant.now().minusSeconds(60))));
        new DeleteGuestNoteHandler(repository).handle(new DeleteGuestNoteHandler.Input(hotel, guest, note, "staff"));
        var order = inOrder(repository);
        order.verify(repository).lockNote(hotel, guest, note);
        order.verify(repository).deleteNote(hotel, guest, note);
    }

    @Test
    void missingOrForeignScopedNoteIsNotDeleted() {
        var repository = mock(GuestCommands.class);
        UUID hotel = UUID.randomUUID(), guest = UUID.randomUUID(), note = UUID.randomUUID();
        when(repository.lockNote(hotel, guest, note)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new DeleteGuestNoteHandler(repository)
            .handle(new DeleteGuestNoteHandler.Input(hotel, guest, note, "staff")))
            .isInstanceOfSatisfying(BusinessException.class, error -> assertThat(error.kind()).isEqualTo(BusinessException.Kind.NOT_FOUND));
        verify(repository, never()).deleteNote(hotel, guest, note);
    }

    @Test
    void expiredNoteIsNotDeletedByTheHandler() {
        var repository = mock(GuestCommands.class);
        UUID hotel = UUID.randomUUID(), guest = UUID.randomUUID(), note = UUID.randomUUID();
        when(repository.lockNote(hotel, guest, note)).thenReturn(Optional.of(
            new GuestNote(note, "Note", "staff", Instant.now().minus(Duration.ofHours(25)))));
        assertThatThrownBy(() -> new DeleteGuestNoteHandler(repository)
            .handle(new DeleteGuestNoteHandler.Input(hotel, guest, note, "staff"))).isInstanceOf(BusinessException.class);
        verify(repository, never()).deleteNote(hotel, guest, note);
    }
}
