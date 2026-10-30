import { Component, input, output, signal } from '@angular/core';
import type { Booking } from '@crm/models/booking.model';
import { label } from '@crm/utils/reservation-status';
import { money } from '@shared/utils/currency';
import { StayDatesComponent } from '@shared/components/stay-dates/stay-dates.component';
import type { PhysicalRoom, RoomAssignment } from '../../models/pms.model';
@Component({
    selector: 'pms-stay-list', standalone: true, imports: [StayDatesComponent],
    templateUrl: './stay-list.component.html', styleUrl: './stay-list.component.css'
})
export class StayListComponent {
    readonly bookings = input.required<Booking[]>();
    readonly rooms = input.required<PhysicalRoom[]>();
    readonly assignments = input.required<RoomAssignment[]>();
    readonly busy = input(false);
    readonly assigned = output<{
        reservation: string;
        room: string;
    }>();
    readonly opened = output<Booking>();
    readonly servicesOpened = output<Booking>();
    readonly choices = signal<Record<string, string>>({});
    readonly label = label;
    readonly money = money;
    id(booking: Booking) {
        return booking.id || booking.reservation_id || '';
    }
    assignment(booking: Booking) {
        return this.assignments().find(row => row.reservationId === this.id(booking));
    }
    candidates(booking: Booking) {
        return this.rooms().filter(room => room.roomTypeId === booking.room_type_id);
    }
    roomNumber(booking: Booking) {
        return this.rooms().find(room => room.id === this.assignment(booking)?.roomId)?.number || 'Atanmamış';
    }
    pick(booking: Booking, room: string) {
        this.choices.update(current => ({ ...current, [this.id(booking)]: room }));
    }
    save(booking: Booking) {
        const reservation = this.id(booking);
        const room = this.choices()[reservation];
        if (!room)
            return;
        this.assigned.emit({ reservation, room });
        this.choices.update(current => {
            const next = { ...current };
            delete next[reservation];
            return next;
        });
    }
}
