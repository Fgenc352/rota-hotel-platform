package com.hotelbooking.inventory.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room_type")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RoomTypeEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "rooms", nullable = false)
    private int rooms;

    @Column(name = "nightly_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal nightlyPrice;

    @Column(name = "weekend_multiplier", nullable = false, precision = 5, scale = 2)
    private BigDecimal weekendMultiplier;

    @Column(name = "min_nights", nullable = false)
    private int minNights;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "area_square_meters", nullable = false)
    private int areaSquareMeters;

    @Column(name = "bed_description", nullable = false, length = 160)
    private String bedDescription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "amenities", nullable = false, columnDefinition = "jsonb")
    private List<String> amenities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "images", nullable = false, columnDefinition = "jsonb")
    private List<String> images;
}
