import { Component, computed, input, output, signal } from '@angular/core';
import { housekeepingLabel, type PhysicalRoom } from '../../models/pms.model';
@Component({
    selector: 'pms-daily-room-summary', standalone: true,
    templateUrl: './daily-room-summary.component.html', styleUrl: './daily-room-summary.component.css'
})
export class DailyRoomSummaryComponent {
    readonly rooms = input.required<PhysicalRoom[]>();
    readonly opened = output<string>();
    readonly filter = signal('ALL');
    readonly page = signal(0);
    readonly housekeeping = housekeepingLabel;
    readonly filtered = computed(() => this.rooms().filter(room => this.filter() === 'ALL' || (this.filter() === 'OCCUPIED' ? !!room.occupantId : !room.occupantId)));
    readonly visible = computed(() => this.filtered().slice(this.page() * 12, (this.page() + 1) * 12));
    scope(value: string) {
        this.filter.set(value);
        this.page.set(0);
    }
}
