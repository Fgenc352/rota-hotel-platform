package com.hotelbooking.inventory.application.port;

import com.hotelbooking.inventory.domain.Room;
import com.hotelbooking.shared.domain.Stay;
import java.time.LocalDate;
import java.util.UUID;

public interface InventoryCommands {
    Room room(UUID hotel, UUID room);

    void reserve(Room room, Stay stay);

    void release(UUID hotel, UUID room, LocalDate in, LocalDate out);
}
