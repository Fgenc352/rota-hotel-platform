import { Component, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { ActionState } from '@shared/state/action-state.service';
import { HotelTimestampPipe } from '@shared/pipes/hotel-timestamp.pipe';
import { StayDatesComponent } from '@shared/components/stay-dates/stay-dates.component';
import { label } from '@crm/utils/reservation-status';
import { DailyRoomSummaryComponent } from '../daily-room-summary/daily-room-summary.component';
import type { PhysicalRoom } from '../../models/pms.model';
import { DailyOperationsStore } from '../../state/daily-operations.store';
import { dailyStatusLabel } from '../../models/daily-operations.model';
import { DailyServiceEditorComponent } from '../daily-service-editor/daily-service-editor.component';
@Component({
    selector: 'pms-daily-operations', standalone: true,
    providers: [ActionState, DailyOperationsStore],
    imports: [
        DailyRoomSummaryComponent, DailyServiceEditorComponent, StayDatesComponent, HotelTimestampPipe
    ],
    templateUrl: './daily-operations.component.html', styleUrl: './daily-operations.component.css'
})
export class DailyOperationsComponent {
    readonly store = inject(DailyOperationsStore);
    readonly rooms = input.required<PhysicalRoom[]>();
    readonly opened = output<string>();
    readonly refreshed = output<void>();
    readonly query = signal('');
    readonly stayFilter = signal('ALL');
    readonly serviceFilter = signal('ALL');
    readonly pendingOnly = signal(false);
    readonly stayPage = signal(0);
    readonly servicePage = signal(0);
    readonly transferPage = signal(0);
    readonly upcoming = computed(() => (this.store.data()?.upcomingTransfers || []).filter(item => this.matches(item.guestName, item.bookingCode, item.roomNumber)));
    readonly visibleUpcoming = computed(() => this.upcoming().slice(this.transferPage() * 20, (this.transferPage() + 1) * 20));
    readonly label = label;
    readonly statusLabel = dailyStatusLabel;
    readonly stays = computed(() => (this.store.data()?.stays || []).filter(stay => this.matches(stay.guestName, stay.code, stay.roomNumber)
        && (this.stayFilter() === 'ALL' || (this.stayFilter() === 'IN_HOUSE' ? stay.status === 'CHECKED_IN'
            : this.stayFilter() === 'ARRIVAL' ? stay.arrival : this.stayFilter() === 'DEPARTURE' ? stay.departure
                : this.stayFilter() === 'UNASSIGNED' ? stay.roomNumber === 'Atanmamış' : false))));
    readonly services = computed(() => (this.store.data()?.services || []).filter(item => this.matches(item.guestName, item.bookingCode, item.roomNumber)
        && (this.serviceFilter() === 'ALL' || item.task.code === this.serviceFilter())
        && (!this.pendingOnly() || !['SERVED', 'COMPLETED', 'DELIVERED'].includes(item.task.status))));
    readonly visibleStays = computed(() => this.stays().slice(this.stayPage() * 20, (this.stayPage() + 1) * 20));
    readonly visibleServices = computed(() => this.services().slice(this.servicePage() * 12, (this.servicePage() + 1) * 12));
    readonly peopleInHouse = computed(() => (this.store.data()?.stays || []).filter(stay => stay.status === 'CHECKED_IN').reduce((total, stay) => total + stay.people, 0));
    readonly inHouse = computed(() => (this.store.data()?.stays || []).filter(stay => stay.status === 'CHECKED_IN').length);
    constructor() {
        // Room-board refreshes also refresh daily metrics after check-in/out and manual reloads.
        effect(() => {
            this.rooms();
            untracked(() => {
                if (!this.store.selected() && !this.store.actions.busy())
                    void this.store.refresh();
            });
        });
    }
    refresh() {
        this.refreshed.emit();
        void this.store.refresh();
    }
    day(date: string) {
        this.reset();
        void this.store.day(date);
    }
    dateLabel(value: string) {
        return new Intl.DateTimeFormat('tr-TR', {
            day: 'numeric', month: 'long', year: 'numeric'
        }).format(new Date(value + 'T12:00:00'));
    }
    today() {
        this.day('');
    }
    reset() {
        this.stayPage.set(0);
        this.servicePage.set(0);
        this.transferPage.set(0);
    }
    search(value: string) {
        this.query.set(value);
        this.reset();
    }
    private matches(...values: string[]) {
        return values.join(' ').toLocaleLowerCase('tr').includes(this.query().trim().toLocaleLowerCase('tr'));
    }
}
