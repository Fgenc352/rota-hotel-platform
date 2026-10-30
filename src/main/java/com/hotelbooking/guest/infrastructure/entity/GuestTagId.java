package com.hotelbooking.guest.infrastructure.entity;

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
public class GuestTagId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;

    @Column(name = "guest_id", nullable = false)
    private UUID guestId;

    @Column(name = "tag", nullable = false, length = 40)
    private String tag;
}
