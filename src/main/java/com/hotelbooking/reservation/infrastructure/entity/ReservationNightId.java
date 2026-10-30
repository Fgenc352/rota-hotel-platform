package com.hotelbooking.reservation.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ReservationNightId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "stay_date", nullable = false)
    private LocalDate stayDate;
}
