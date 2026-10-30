package com.hotelbooking.hotel.infrastructure.mapper;

import com.hotelbooking.hotel.domain.Hotel;
import com.hotelbooking.hotel.infrastructure.entity.HotelEntity;
import org.springframework.stereotype.Component;

@Component
public class HotelEntityMapper {
    public Hotel toDomain(HotelEntity entity) {
        return new Hotel(entity.getId(), entity.getPublicKey(), entity.getName(), entity.getLocation(), entity.getTimezone());
    }
}
