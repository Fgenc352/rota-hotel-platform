package com.hotelbooking.inventory.application.port;

import com.hotelbooking.inventory.domain.Room;
import com.hotelbooking.shared.domain.Stay;
import java.util.List;
import java.util.UUID;

public interface AvailabilityQueries {
    long version(UUID hotel);

    List<Room> rooms(UUID hotel, Stay stay);

    int sold(UUID hotel, UUID room, Stay stay);
}
