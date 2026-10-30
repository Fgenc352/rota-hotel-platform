package com.hotelbooking.inventory.application.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RoomDetails(UUID id, String name, String description, int capacity,
    int areaSquareMeters, String bedDescription, BigDecimal nightlyPrice,
    String currency, int minNights, List<String> amenities, List<String> images) {
}
