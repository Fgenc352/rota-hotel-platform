package com.hotelbooking.reservation.infrastructure.entity;

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
public class IdempotencyId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;

    @Column(name = "key", nullable = false, length = 100)
    private String key;
}
