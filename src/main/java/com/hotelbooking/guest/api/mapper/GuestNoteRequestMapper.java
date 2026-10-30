package com.hotelbooking.guest.api.mapper;

import com.hotelbooking.guest.api.dto.CreateNoteRequest;
import com.hotelbooking.guest.application.command.AddGuestNoteHandler;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GuestNoteRequestMapper {
    public AddGuestNoteHandler.Input toCommand(UUID hotel, UUID guest, String author, CreateNoteRequest request) {
        return new AddGuestNoteHandler.Input(hotel, guest, author, request.body());
    }
}
