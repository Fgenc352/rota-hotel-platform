package com.hotelbooking.inventory.application.mapper;

import com.hotelbooking.inventory.application.model.Night;
import com.hotelbooking.inventory.domain.Room;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class NightMapper {
    public Night toNight(Room room, LocalDate date) {
        return new Night(date, room.price(date));
    }
}
