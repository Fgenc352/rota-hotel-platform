package com.hotelbooking.inventory.api;

import com.hotelbooking.inventory.application.model.RoomDetails;
import com.hotelbooking.inventory.application.query.GetRoomCatalogHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/hotels/{key}/rooms")
@RequiredArgsConstructor
public class RoomCatalogController {
    private final GetRoomCatalogHandler rooms;

    @GetMapping
    List<RoomDetails> rooms(@PathVariable String key) {
        return rooms.handle(key);
    }
}
