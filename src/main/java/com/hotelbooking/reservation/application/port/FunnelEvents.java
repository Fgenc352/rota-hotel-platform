package com.hotelbooking.reservation.application.port;

import java.util.UUID;

public interface FunnelEvents {
    void record(UUID hotel, UUID session, String stage);
}
