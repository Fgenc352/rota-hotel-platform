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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "pms_daily_service_audit")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PmsDailyServiceAuditEntity {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "hotel_id")
    private UUID hotelId;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "code")
    private String code;

    @Column(name = "service_date")
    private LocalDate date;

    @Column(name = "actor")
    private String actor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_state")
    private String beforeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_state")
    private String afterState;

    @Column(name = "created_at")
    private Instant createdAt;

}
