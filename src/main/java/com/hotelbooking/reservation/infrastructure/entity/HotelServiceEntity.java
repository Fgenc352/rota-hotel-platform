package com.hotelbooking.reservation.infrastructure.entity;

import com.hotelbooking.reservation.domain.ServiceUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hotel_service")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HotelServiceEntity {
    @Id
    private UUID id;
    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;
    private String code;
    private String name;
    private String description;
    private BigDecimal price;
    @Enumerated(EnumType.STRING)
    private ServiceUnit unit;
    private boolean included;
    private boolean active;
}
