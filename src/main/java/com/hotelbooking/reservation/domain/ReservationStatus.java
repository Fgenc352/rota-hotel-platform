package com.hotelbooking.reservation.domain;

public enum ReservationStatus {
    PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED, NO_SHOW;
    public boolean canTransitionTo(ReservationStatus next) {
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == CHECKED_IN || next == CANCELLED || next == NO_SHOW;
            case CHECKED_IN -> next == CHECKED_OUT;
            default -> false;
        }
        ;
    }

    public boolean releasesInventory() {
        return this == CANCELLED || this == NO_SHOW;
    }
}
