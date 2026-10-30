package com.hotelbooking.guest.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNoteRequest( @NotBlank @Size(max = 2000) String body) {
}
