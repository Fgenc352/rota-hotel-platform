package com.hotelbooking.pms.infrastructure.entity;

import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.hotelbooking.pms.domain.ServiceProgress;

@Entity
@Table(name = "pms_service_audit")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PmsServiceAuditEntity {
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
    @Column(name = "previous_state", nullable = false, length = 20)
    private ServiceProgress previousState;

    @Enumerated(EnumType.STRING)
    @Column(name = "next_state", nullable = false, length = 20)
    private ServiceProgress nextState;

    @Column(name = "actor", nullable = false, length = 120)
    private String actor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
