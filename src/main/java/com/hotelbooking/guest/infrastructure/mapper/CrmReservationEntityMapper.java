package com.hotelbooking.guest.infrastructure.mapper;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.guest.application.model.AppliedReservation;
import com.hotelbooking.guest.application.model.GuestReservation;
import com.hotelbooking.guest.infrastructure.entity.CrmReservationEntity;
import com.hotelbooking.guest.infrastructure.entity.CrmReservationId;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CrmReservationEntityMapper {
    public AppliedReservation toApplied(CrmReservationEntity entity) {
        return new AppliedReservation(entity.getGuestId(), entity.getVersion());
    }

    public GuestReservation toModel(CrmReservationEntity entity) {
        return new GuestReservation(entity.getId().getHotelId(), entity.getId().getReservationId(), entity.getGuestId(),
            entity.getCode(), entity.getCheckIn(), entity.getCheckOut(), entity.getStatus(), entity.getTotal(),
            entity.getVersion(), entity.getCheckedOutAt());
    }

    public CrmReservationEntity toEntity(UUID guest, ReservationSnapshot event) {
        return CrmReservationEntity.builder().id(new CrmReservationId(event.hotelId(), event.id())).guestId(guest)
            .code(event.code()).checkIn(event.checkIn()).checkOut(event.checkOut()).status(event.status()).total(event.total())
            .version(event.version()).checkedOutAt(event.checkedOutAt()).build();
    }
}
