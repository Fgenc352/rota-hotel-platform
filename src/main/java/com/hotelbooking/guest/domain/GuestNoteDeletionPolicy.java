package com.hotelbooking.guest.domain;

import com.hotelbooking.shared.exception.BusinessException;
import java.time.Duration;
import java.time.Instant;

public final class GuestNoteDeletionPolicy {
    private static final Duration DELETION_WINDOW = Duration.ofHours(24);

    private GuestNoteDeletionPolicy() {
    }

    public static void validate(String author, Instant createdAt, String actor, Instant now) {
        if (!author.equals(actor)) {
            throw BusinessException.forbidden("NOTE_AUTHOR_REQUIRED", "Notu yalnızca yazan personel silebilir.");
        }
        if (now.isBefore(createdAt) || !now.isBefore(createdAt.plus(DELETION_WINDOW))) {
            throw BusinessException.conflict("NOTE_DELETE_WINDOW_CLOSED", "Notlar yalnızca eklendikten sonraki ilk 24 saat içinde silinebilir.");
        }
    }
}
