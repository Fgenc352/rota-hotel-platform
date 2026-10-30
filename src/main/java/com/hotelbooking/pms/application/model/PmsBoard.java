package com.hotelbooking.pms.application.model;

import com.hotelbooking.pms.domain.RoomAssignment;
import java.util.List;

public record PmsBoard(List<RoomView> rooms, List<RoomAssignment> assignments) {
}
