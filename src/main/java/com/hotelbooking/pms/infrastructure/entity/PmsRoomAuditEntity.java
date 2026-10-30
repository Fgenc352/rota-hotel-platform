package com.hotelbooking.pms.infrastructure.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "pms_room_audit")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PmsRoomAuditEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "action", nullable = false, length = 40)
    private String action;

    @Column(name = "actor", nullable = false, length = 120)
    private String actor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state", nullable = false)
    private String beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state", nullable = false)
    private String afterState;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
