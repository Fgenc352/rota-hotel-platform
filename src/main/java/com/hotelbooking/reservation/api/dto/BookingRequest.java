package com.hotelbooking.reservation.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record BookingRequest(
    @NotNull UUID roomTypeId,
    @NotNull LocalDate checkIn,
    @NotNull LocalDate checkOut,
    @Min(1) @Max(10) int adults,
    @Min(0) @Max(10) int children,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Pattern(regexp = "\\+[1-9][0-9]{7,14}", message = "Telefon +905551234567 biçiminde olmalıdır.") String phone,
    @AssertTrue(message = "KVKK aydınlatma onayı gereklidir.") boolean privacyAccepted,
    boolean marketingAllowed,
    UUID sessionId,
    @Size(max = 20) List<@NotBlank @Size(max = 40) String> serviceCodes
    ) {
    public BookingRequest {
        serviceCodes = serviceCodes == null?List.of():Collections.unmodifiableList(new ArrayList<>(serviceCodes));
    }

    public BookingRequest(UUID roomTypeId, LocalDate checkIn, LocalDate checkOut, int adults, int children,
        String firstName, String lastName, String email, String phone, boolean privacyAccepted,
        boolean marketingAllowed, UUID sessionId) {
        this(roomTypeId, checkIn, checkOut, adults, children, firstName, lastName, email, phone, privacyAccepted,
            marketingAllowed, sessionId, List.of());
    }
}
