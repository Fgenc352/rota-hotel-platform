package com.hotelbooking.inventory.application.port;

import com.hotelbooking.inventory.application.model.RoomDetails;
import java.util.List;
import java.util.UUID;

public interface RoomCatalog {
    List<RoomDetails> rooms(UUID hotel);
}
