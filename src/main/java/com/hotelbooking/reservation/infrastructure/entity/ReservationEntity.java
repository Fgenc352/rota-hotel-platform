package com.hotelbooking.reservation.infrastructure.entity;

import com.hotelbooking.reservation.domain.ServiceLine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "reservation")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReservationEntity {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;

    @Column(name = "room_type_id", nullable = false)
    private UUID roomTypeId;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(name = "adults", nullable = false)
    private int adults;

    @Column(name = "children", nullable = false)
    private int children;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "privacy_accepted", nullable = false)
    private boolean privacyAccepted;

    @Column(name = "marketing_allowed", nullable = false)
    private boolean marketingAllowed;

    @Column(name = "consent_version", nullable = false, length = 30)
    private String consentVersion;

    @Column(name = "consent_at", nullable = false)
    private Instant consentAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "checked_out_at", nullable = true)
    private Instant checkedOutAt;

    @Column(name = "room_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal roomTotal;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "service_lines", nullable = false, columnDefinition = "jsonb")
    private List<ServiceLine> serviceLines;

    public void recordServices(BigDecimal roomTotal, List<ServiceLine> lines) {
        this.roomTotal = roomTotal;
        this.serviceLines = List.copyOf(lines);
    }

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    public void changeStatus(String status, Instant now) {
        this.status = status;
        version++;
        if ("CHECKED_OUT".equals(status)) {
            checkedOutAt = now;
        }
    }
}
