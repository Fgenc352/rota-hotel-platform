package com.hotelbooking.hotel.application.port;

import com.hotelbooking.hotel.domain.Hotel;
import java.util.UUID;

public interface HotelCatalog {
    Hotel byKey(String key);

    Hotel byId(UUID id);

    void lock(UUID id);

    String timezone(UUID hotel);

    boolean allowsOrigin(String key, String origin);
}
