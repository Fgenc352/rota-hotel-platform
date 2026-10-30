package com.hotelbooking.identity.infrastructure.mapper;

import com.hotelbooking.hotel.domain.Hotel;
import com.hotelbooking.identity.application.model.StaffCredentials;
import com.hotelbooking.identity.application.model.StaffIdentity;
import com.hotelbooking.identity.infrastructure.entity.StaffUserEntity;
import org.springframework.stereotype.Component;

@Component
public class StaffEntityMapper {
    public StaffCredentials toCredentials(StaffUserEntity entity) {
        return new StaffCredentials(entity.getUsername(), entity.getPasswordHash());
    }

    public StaffIdentity toIdentity(StaffUserEntity entity, Hotel hotel) {
        return new StaffIdentity(entity.getUsername(), hotel.id(), hotel.key(), hotel.name(), hotel.location());
    }
}
