package com.hotelbooking.pms.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pms_room_assignment")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PmsAssignmentEntity {
    @Id
    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    public void move(UUID room, LocalDate in, LocalDate out, String next) {
        roomId = room;
        checkIn = in;
        checkOut = out;
        status = next;
    }
    public void changeStatus(String next) {
        status = next;
    }
}
