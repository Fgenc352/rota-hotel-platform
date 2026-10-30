package com.hotelbooking.guest.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSegmentRequest( @NotBlank @Size(max = 100) String name, @Min(0) @Max(100) int minStays,
    @Min(1) @Max(120) int months, boolean marketingRequired) {
}
