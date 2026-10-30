package com.hotelbooking.guest.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "guest_match_audit")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GuestMatchAuditEntity {
    @Id
    private UUID id;
    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;
    @Column(name = "source_guest_id", nullable = false)
    private UUID sourceGuestId;
    @Column(name = "target_guest_id", nullable = false)
    private UUID targetGuestId;
    @Column(name = "resolved_by", nullable = false, length = 120)
    private String resolvedBy;
    @Column(name = "resolved_at", nullable = false)
    private Instant resolvedAt;

    @Column(name = "snapshot_version", nullable = false)
    private int snapshotVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state", columnDefinition = "jsonb")
    private String beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state", columnDefinition = "jsonb")
    private String afterState;
}
