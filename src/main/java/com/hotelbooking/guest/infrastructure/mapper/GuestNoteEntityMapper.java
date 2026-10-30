package com.hotelbooking.guest.infrastructure.mapper;

import com.hotelbooking.guest.application.model.GuestNote;
import com.hotelbooking.guest.infrastructure.entity.GuestNoteEntity;
import org.springframework.stereotype.Component;

@Component
public class GuestNoteEntityMapper {
    public GuestNote toModel(GuestNoteEntity entity) {
        return new GuestNote(entity.getId(), entity.getBody(), entity.getAuthor(), entity.getCreatedAt());
    }
}
