package com.hotelbooking.pms.domain;

import com.hotelbooking.shared.exception.BusinessException;
import java.util.UUID;

public final class RoomRules {
    private RoomRules() {
    }

    public static void assignable(PhysicalRoom room, UUID requiredType, String reservationStatus) {
        if (!room.roomTypeId().equals(requiredType)) {
            throw BusinessException.conflict("ROOM_TYPE_MISMATCH", "Rezervasyondaki oda tipine uygun bir oda seçin.");
        }
        if (!reservationStatus.equals("CONFIRMED") && !reservationStatus.equals("CHECKED_IN")) {
            throw BusinessException.conflict("ROOM_ASSIGNMENT_CLOSED", "Yalnızca onaylı veya giriş yapmış rezervasyona oda atanabilir.");
        }
    }

    public static void readyForCheckIn(PhysicalRoom room) {
        if (room.housekeeping() != Housekeeping.CLEAN) {
            throw BusinessException.conflict("ROOM_NOT_READY", "Oda henüz hazır değil. Önce temizlik/kontrol işlemini tamamlayın.");
        }
    }
}
