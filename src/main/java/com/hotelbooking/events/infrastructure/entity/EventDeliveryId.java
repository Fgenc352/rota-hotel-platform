package com.hotelbooking.events.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EventDeliveryId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "consumer", nullable = false, length = 30)
    private String consumer;
}
