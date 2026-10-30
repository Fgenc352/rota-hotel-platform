package com.hotelbooking.inventory.infrastructure;

import com.hotelbooking.inventory.application.port.InventoryCommands;
import com.hotelbooking.inventory.domain.Room;
import com.hotelbooking.inventory.infrastructure.jpa.RoomTypeJpaRepository;
import com.hotelbooking.inventory.infrastructure.mapper.RoomEntityMapper;
import com.hotelbooking.shared.domain.Stay;
import com.hotelbooking.shared.exception.BusinessException;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@Transactional
public class InventoryCommandRepository implements InventoryCommands {
    private final EntityManager entityManager;
    private final JdbcTemplate jdbc;
    private final RoomTypeJpaRepository rooms;
    private final RoomEntityMapper mapper;

    public Room room(UUID hotel, UUID room) {
        return mapper.toDomain(rooms.findByHotelIdAndId(hotel, room).orElseThrow(BusinessException::missing));
    }
    public void reserve(Room room, Stay stay) {
        // Lock each occupied night in date order. Different dates can be booked concurrently.
        for (var date : stay.dates().stream().sorted().toList()) {
            jdbc.update("""
                INSERT INTO room_inventory(hotel_id,room_type_id,stay_date,capacity,sold)
                VALUES (?,?,?,?,0) ON CONFLICT (hotel_id,room_type_id,stay_date) DO NOTHING
                """, room.hotelId(), room.id(), date, room.rooms());
            var inventory = jdbc.queryForMap("""
                SELECT capacity,sold FROM room_inventory
                WHERE hotel_id=? AND room_type_id=? AND stay_date=? FOR UPDATE
                """, room.hotelId(), room.id(), date);
            int capacity = (int) inventory.get("capacity");
            int sold = (int) inventory.get("sold");
            if (sold >= capacity) {
                throw BusinessException.conflict("SOLD_OUT", "Seçilen oda az önce doldu. Lütfen yeniden arayın.");
            }
            jdbc.update("UPDATE room_inventory SET sold=sold+1 WHERE hotel_id=? AND room_type_id=? AND stay_date=?",
                room.hotelId(), room.id(), date);
        }
        entityManager.flush();
    }
    public void release(UUID hotel, UUID room, LocalDate in, LocalDate out) {
        for (var date = in; date.isBefore(out); date = date.plusDays(1)) {
            var rows = jdbc.queryForList("""
                SELECT sold FROM room_inventory
                WHERE hotel_id=? AND room_type_id=? AND stay_date=? FOR UPDATE
                """, hotel, room, date);
            if (rows.size() != 1 || ((Number) rows.getFirst().get("sold")).intValue() <= 0) {
                throw new IllegalStateException("Inventory release invariant violated");
            }
            jdbc.update("UPDATE room_inventory SET sold=sold-1 WHERE hotel_id=? AND room_type_id=? AND stay_date=?",
                hotel, room, date);
        }
        entityManager.flush();
    }
}
