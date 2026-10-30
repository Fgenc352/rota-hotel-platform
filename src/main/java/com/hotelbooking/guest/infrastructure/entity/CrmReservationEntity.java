package com.hotelbooking.guest.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "crm_reservation")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CrmReservationEntity {
    @EmbeddedId
    private CrmReservationId id;

    @Column(name = "guest_id", nullable = false)
    private UUID guestId;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "checked_out_at", nullable = true)
    private Instant checkedOutAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", referencedColumnName = "id", insertable = false, updatable = false)
    private GuestEntity guest;

    public void apply(String status, BigDecimal total, int version, Instant checkedOutAt) {
        this.status = status;
        this.total = total;
        this.version = version;
        this.checkedOutAt = checkedOutAt;
    }
}
