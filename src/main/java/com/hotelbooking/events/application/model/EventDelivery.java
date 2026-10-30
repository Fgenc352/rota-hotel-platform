package com.hotelbooking.events.application.model;

import java.util.UUID;

public record EventDelivery(UUID id, String type, String traceId, String payload) {
}
