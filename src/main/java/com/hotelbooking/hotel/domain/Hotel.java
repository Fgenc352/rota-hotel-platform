package com.hotelbooking.hotel.domain;

import java.util.UUID;

public record Hotel( UUID id, String key, String name, String location, String timezone) {
}
