package com.hotelbooking.pms.infrastructure.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.hotelbooking.pms.domain.ServiceProgress;

@Entity
@Table(name = "pms_service_task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PmsServiceTaskEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;

    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "progress", nullable = false, length = 20)
    private ServiceProgress progress;

    @Column(name = "updated_by", nullable = false, length = 120)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;
}
