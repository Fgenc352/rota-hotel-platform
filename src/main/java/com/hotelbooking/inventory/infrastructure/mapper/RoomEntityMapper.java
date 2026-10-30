package com.hotelbooking.inventory.infrastructure.mapper;

import com.hotelbooking.inventory.domain.Room;
import com.hotelbooking.inventory.infrastructure.entity.RoomTypeEntity;
import org.springframework.stereotype.Component;

@Component
public class RoomEntityMapper {
    public Room toDomain(RoomTypeEntity entity) {
        return new Room(entity.getId(), entity.getHotelId(), entity.getName(), entity.getCapacity(), entity.getRooms(),
            entity.getNightlyPrice(), entity.getWeekendMultiplier(), entity.getMinNights());
    }
}
