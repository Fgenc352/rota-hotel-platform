package com.hotelbooking.pms.application.query;

import com.hotelbooking.inventory.application.port.RoomCatalog;
import com.hotelbooking.pms.application.model.PmsBoard;
import com.hotelbooking.pms.application.model.RoomView;
import com.hotelbooking.pms.application.port.RoomBoardQueries;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetPmsBoardHandler {
    private final RoomBoardQueries queries;
    private final RoomCatalog catalog;

    public PmsBoard handle(UUID hotel) {
        var names = catalog.rooms(hotel).stream().collect(Collectors.toMap(room -> room.id(), room -> room.name()));
        var rooms = queries.rooms(hotel).stream().map(room -> new RoomView(room.id(), room.roomTypeId(),
            names.getOrDefault(room.roomTypeId(), "Oda"), room.number(), room.housekeeping(), room.occupantId(),
            room.occupantCode(), room.upcomingAssignments())).toList();
        return new PmsBoard(rooms, queries.assignments(hotel));
    }
}
