package com.hotelbooking.pms.application.command;

import com.hotelbooking.pms.application.port.RoomOperations;
import com.hotelbooking.pms.domain.Housekeeping;
import com.hotelbooking.shared.logging.TransactionLog;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChangeHousekeepingHandler {
    public record Input(UUID hotel, UUID room, Housekeeping state, String actor) {
    }
    private final RoomOperations rooms;

    public void handle(Input input) {
        var room = rooms.lockRoom(input.hotel(), input.room());
        if (room.housekeeping() == input.state()) {
            return;
        }
        rooms.housekeeping(input.hotel(), input.room(), input.state());
        rooms.audit(input.hotel(), input.room(), null, "HOUSEKEEPING_CHANGED", input.actor(),
            Map.of("housekeeping", room.housekeeping()), Map.of("housekeeping", input.state()));
        TransactionLog.afterCommit(() -> log.info("event=pms.housekeeping_changed hotelId={} roomId={} state={}",
            input.hotel(), input.room(), input.state()));
    }
}
