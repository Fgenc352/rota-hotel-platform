package com.hotelbooking.inventory.infrastructure.mapper;

import com.hotelbooking.inventory.application.model.RoomDetails;
import com.hotelbooking.inventory.infrastructure.entity.RoomTypeEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RoomDetailsMapper {
    public RoomDetails toModel(RoomTypeEntity room) {
        return new RoomDetails(room.getId(), room.getName(), room.getDescription(), room.getCapacity(),
            room.getAreaSquareMeters(), room.getBedDescription(), room.getNightlyPrice(), "TRY", room.getMinNights(),
            List.copyOf(room.getAmenities()), List.copyOf(room.getImages()));
    }
}
