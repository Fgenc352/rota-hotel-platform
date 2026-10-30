package com.hotelbooking.inventory.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room_inventory")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RoomInventoryEntity {
    @EmbeddedId
    private RoomInventoryId id;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    @Column(name = "sold", nullable = false)
    private int sold;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", referencedColumnName = "id", insertable = false, updatable = false)
    private RoomTypeEntity room;

    public void reserve() {
        sold++;
    }
    public void release() {
        sold--;
    }
}
