package com.hotelbooking.guest.infrastructure.segment;

final class SegmentSql {
    static final String TODAY = "(now() AT TIME ZONE (SELECT timezone FROM hotel WHERE id=g.hotel_id))::date";
    static final String CHECKOUT = "(r.checked_out_at AT TIME ZONE (SELECT timezone FROM hotel WHERE id=g.hotel_id))::date";

    private SegmentSql() {
    }
}
