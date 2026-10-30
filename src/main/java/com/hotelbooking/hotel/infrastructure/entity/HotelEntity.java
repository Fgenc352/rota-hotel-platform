package com.hotelbooking.hotel.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "hotel")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HotelEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "public_key", nullable = false, length = 80)
    private String publicKey;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "location", nullable = false, length = 100)
    private String location;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone;

}
