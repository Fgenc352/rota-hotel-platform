package com.hotelbooking.events.application.port;

import java.util.UUID;

public interface EventRetries {
    boolean retry(UUID hotel, UUID id);
}
