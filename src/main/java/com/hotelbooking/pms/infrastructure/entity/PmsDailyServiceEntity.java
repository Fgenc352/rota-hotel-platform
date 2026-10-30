package com.hotelbooking.pms.infrastructure.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.hotelbooking.pms.domain.DailyServiceStatus;

@Entity
@Table(name = "pms_daily_service")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PmsDailyServiceEntity {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "hotel_id")
    private UUID hotelId;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "service_date")
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DailyServiceStatus status;

    @Column(name = "service_time")
    private LocalTime time;

    @Column(name = "pickup", nullable = false, length = 200)
    private String pickup;

    @Column(name = "destination", nullable = false, length = 200)
    private String destination;

    @Column(name = "plate", nullable = false, length = 30)
    private String plate;

    @Column(name = "notes", nullable = false, length = 1000)
    private String notes;

    @Column(name = "passengers")
    private int passengers;

    @Column(name = "actor", nullable = false, length = 120)
    private String actor;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(name = "lock_version")
    private long lockVersion;

    public void update(com.hotelbooking.pms.application.model.DailyServiceTask value) {
        date = value.date();
        status = value.status();
        time = value.time();
        pickup = value.pickup();
        destination = value.destination();
        plate = value.plate();
        notes = value.notes();
        passengers = value.passengers();
        actor = value.actor();
        updatedAt = value.updatedAt();
    }
}
