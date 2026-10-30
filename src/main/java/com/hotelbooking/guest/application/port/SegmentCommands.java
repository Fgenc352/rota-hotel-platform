package com.hotelbooking.guest.application.port;

import java.util.UUID;

public interface SegmentCommands {
    boolean insert(UUID id, UUID hotel, String name, int minStays, int months, boolean marketingRequired);

    boolean delete(UUID hotel, UUID id);
}
