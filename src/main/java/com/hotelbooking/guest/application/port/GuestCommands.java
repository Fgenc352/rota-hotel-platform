package com.hotelbooking.guest.application.port;

import com.hotelbooking.guest.application.model.GuestNote;
import java.util.Optional;
import java.util.UUID;

public interface GuestCommands {
    boolean exists(UUID hotel, UUID id);

    void insertNote(UUID note, UUID hotel, UUID guest, String author, String body);

    Optional<GuestNote> lockNote(UUID hotel, UUID guest, UUID note);

    void deleteNote(UUID hotel, UUID guest, UUID note);

    void tag(UUID hotel, UUID guest, String tag);

    void untag(UUID hotel, UUID guest, String tag);
}
