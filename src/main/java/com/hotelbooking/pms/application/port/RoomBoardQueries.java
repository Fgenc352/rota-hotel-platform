package com.hotelbooking.pms.application.port;

import com.hotelbooking.pms.application.model.RoomAuditPage;
import com.hotelbooking.pms.application.model.RoomView;
import com.hotelbooking.pms.domain.RoomAssignment;
import java.util.List;
import java.util.UUID;

public interface RoomBoardQueries {
    RoomAuditPage historyPage(UUID hotel, int page, int size);
    List<RoomView> rooms(UUID hotel);
    List<RoomAssignment> assignments(UUID hotel);
    List<RoomAssignment> dailyAssignments(UUID hotel, java.time.LocalDate date);
}
