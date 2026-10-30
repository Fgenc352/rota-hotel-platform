package com.hotelbooking.analytics.application.model;

import java.util.List;

public record Analytics(List<FunnelCount> funnel, long reservations, List<DeliverySummary> deliveries) {
}
