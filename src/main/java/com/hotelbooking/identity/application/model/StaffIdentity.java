package com.hotelbooking.identity.application.model;

import java.util.UUID;

public record StaffIdentity(String username, UUID hotelId, String hotelKey, String hotelName, String location) {
}
