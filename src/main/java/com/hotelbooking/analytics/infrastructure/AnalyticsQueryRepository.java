package com.hotelbooking.analytics.infrastructure;

import com.hotelbooking.analytics.application.model.Analytics;
import com.hotelbooking.analytics.application.port.AnalyticsQueries;
import com.hotelbooking.analytics.infrastructure.mapper.DeliverySummaryMapper;
import com.hotelbooking.analytics.infrastructure.mapper.FunnelCountMapper;
import com.hotelbooking.shared.persistence.JpaQueries;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsQueryRepository implements AnalyticsQueries {
    private final JpaQueries projections;
    private final FunnelCountMapper funnelMapper;
    private final DeliverySummaryMapper deliveryMapper;

    public Analytics analytics(UUID hotel) {
        var parameters = Map.of("hotel", hotel);
        var funnel = projections.tuples("SELECT stage,count(DISTINCT session_id) AS sessions FROM funnel_event WHERE hotel_id=:hotel GROUP BY stage",
            parameters)
            .stream().map(funnelMapper::toModel).toList();
        var deliveries = projections.tuples("""
            SELECT d.consumer,count(*) FILTER(WHERE d.processed_at IS NULL) AS pending,count(*) FILTER(WHERE d.dead) AS dead
            FROM event_delivery d JOIN outbox_event e ON e.id=d.event_id WHERE e.hotel_id=:hotel GROUP BY d.consumer
            """,
            parameters).stream().map(deliveryMapper::toModel).toList();
        return new Analytics(funnel, projections.count("SELECT count(*) FROM reservation WHERE hotel_id=:hotel",
            parameters), deliveries);
    }
}
