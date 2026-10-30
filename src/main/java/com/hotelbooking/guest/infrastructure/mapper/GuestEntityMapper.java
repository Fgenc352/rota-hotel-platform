package com.hotelbooking.guest.infrastructure.mapper;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.guest.infrastructure.entity.GuestEntity;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GuestEntityMapper {
    public GuestEntity toEntity(UUID id, ReservationSnapshot event) {
        return GuestEntity.builder().id(id).hotelId(event.hotelId()).firstName(event.firstName()).lastName(event.lastName())
            .email(event.email()).phone(event.phone()).marketingAllowed(event.marketingAllowed()).consentAt(event.consentAt())
            .build();
    }
}
