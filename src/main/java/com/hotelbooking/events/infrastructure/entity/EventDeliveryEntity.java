package com.hotelbooking.events.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "event_delivery")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EventDeliveryEntity {
    @EmbeddedId
    private EventDeliveryId id;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "next_attempt", nullable = false)
    private Instant nextAttempt;

    @Column(name = "processed_at", nullable = true)
    private Instant processedAt;

    @Column(name = "dead", nullable = false)
    private boolean dead;

    @Column(name = "last_error", nullable = true, length = 250)
    private String lastError;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", referencedColumnName = "id", insertable = false, updatable = false)
    private OutboxEventEntity event;

    public void complete(Instant now) {
        processedAt = now;
        lastError = null;
        dead = false;
    }

    public void fail(String error, Instant now) {
        if (processedAt != null) {
            return;
        }
        long delay = Math.min(300, 1L << Math.min(attempts, 8));
        attempts++;
        nextAttempt = now.plusSeconds(delay);
        dead = attempts >= 10;
        lastError = error;
    }

    public void retry(Instant now) {
        dead = false;
        attempts = 0;
        nextAttempt = now;
    }
}
