package com.hotelbooking.inventory.application.query;

import com.hotelbooking.hotel.application.port.HotelCatalog;
import com.hotelbooking.inventory.application.model.RoomDetails;
import com.hotelbooking.inventory.application.port.RoomCatalog;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetRoomCatalogHandler {
    private final HotelCatalog hotels;
    private final RoomCatalog rooms;

    public List<RoomDetails> handle(String key) {
        return rooms.rooms(hotels.byKey(key).id());
    }
}
