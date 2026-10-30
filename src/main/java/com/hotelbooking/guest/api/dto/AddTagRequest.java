package com.hotelbooking.guest.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddTagRequest( @NotBlank @Size(max = 40) String tag) {
}
