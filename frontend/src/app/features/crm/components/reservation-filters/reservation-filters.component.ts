import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import type { ReservationSearch } from '../../models/reservation-search.model';
import type { RoomDetails } from '@booking/models/room-details.model';
@Component({
    selector: 'crm-reservation-filters', standalone: true, imports: [FormsModule],
    templateUrl: './reservation-filters.component.html', styleUrl: './reservation-filters.component.css'
})
export class ReservationFiltersComponent {
    readonly busy = input(false);
    readonly rooms = input<RoomDetails[]>([]);
    readonly changed = output<ReservationSearch>();
    filters: ReservationSearch = {};
    apply() {
        this.changed.emit({ ...this.filters });
    }
    clear() {
        this.filters = {};
        this.apply();
    }
}
