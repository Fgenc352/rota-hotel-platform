package com.hotelbooking.guest.application.model;

import java.time.LocalDate;
import com.hotelbooking.shared.exception.BusinessException;

public record GuestSearch(String firstName, String lastName, String email, String phone,
    LocalDate checkInFrom, LocalDate checkInTo,
    LocalDate checkOutFrom, LocalDate checkOutTo) {
    public GuestSearch {
        if (checkInFrom != null && checkInTo != null && checkInFrom.isAfter(checkInTo)
            || checkOutFrom != null && checkOutTo != null && checkOutFrom.isAfter(checkOutTo)) {
            throw BusinessException.invalid("Başlangıç tarihi bitiş tarihinden sonra olamaz.");
        }
    }
}
