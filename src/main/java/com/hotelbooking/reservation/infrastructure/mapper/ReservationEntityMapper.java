package com.hotelbooking.reservation.infrastructure.mapper;

import com.hotelbooking.contracts.reservation.ReservationSnapshot;
import com.hotelbooking.reservation.application.command.BookingData;
import com.hotelbooking.reservation.domain.ReservationState;
import com.hotelbooking.reservation.domain.ReservationStatus;
import com.hotelbooking.reservation.infrastructure.entity.ReservationEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ReservationEntityMapper {
    public ReservationEntity toEntity(UUID id, UUID hotel, String code, BookingData request, BigDecimal total) {
        return ReservationEntity.builder().id(id).hotelId(hotel).roomTypeId(request.roomTypeId()).code(code)
            .checkIn(request.checkIn()).checkOut(request.checkOut()).adults(request.adults()).children(request.children())
            .firstName(request.firstName()).lastName(request.lastName()).email(request.email()).phone(request.phone())
            .privacyAccepted(true).marketingAllowed(request.marketingAllowed()).consentVersion("privacy-v1")
            .consentAt(Instant.now()).status("CONFIRMED").total(total).roomTotal(total).serviceLines(List.of()).currency("TRY").version(1).createdAt(Instant.now()).build();
    }

    public ReservationState toState(ReservationEntity entity) {
        return new ReservationState(entity.getCode(), ReservationStatus.valueOf(entity.getStatus()),
            entity.getTotal(),
            entity.getCheckIn(), entity.getCheckOut());
    }

    public ReservationSnapshot toSnapshot(ReservationEntity entity) {
        return new ReservationSnapshot(entity.getId(), entity.getHotelId(), entity.getCode(),
            entity.getCheckIn(), entity.getCheckOut(),
            entity.getFirstName(), entity.getLastName(), entity.getEmail(), entity.getPhone(),
            entity.isMarketingAllowed(),
            entity.getConsentAt(), entity.getStatus(), entity.getTotal(), entity.getVersion(),
            entity.getCheckedOutAt());
    }
}
