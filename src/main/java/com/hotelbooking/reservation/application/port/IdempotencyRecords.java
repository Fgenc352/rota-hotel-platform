package com.hotelbooking.reservation.application.port;

import com.hotelbooking.reservation.application.model.IdempotencyEntry;
import java.util.UUID;

public interface IdempotencyRecords {
    IdempotencyEntry lock(UUID hotel, String key, String hash);

    void complete(UUID hotel, String key, String response);
}
