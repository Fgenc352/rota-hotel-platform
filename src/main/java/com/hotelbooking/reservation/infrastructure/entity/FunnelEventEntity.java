package com.hotelbooking.reservation.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "funnel_event")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FunnelEventEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "stage", nullable = false, length = 20)
    private String stage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}
