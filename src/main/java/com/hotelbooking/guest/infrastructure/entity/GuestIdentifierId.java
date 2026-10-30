package com.hotelbooking.guest.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class GuestIdentifierId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    public GuestIdentifierId(UUID hotelId, String email, String phone) {
        this.hotelId = Objects.requireNonNull(hotelId);
        this.email = Objects.requireNonNull(email).strip().toLowerCase(Locale.ROOT);
        this.phone = Objects.requireNonNull(phone);
    }
}
