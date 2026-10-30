package com.hotelbooking.hotel.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
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
public class HotelOriginId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;

    @Column(name = "origin", nullable = false, length = 250)
    private String origin;
}
