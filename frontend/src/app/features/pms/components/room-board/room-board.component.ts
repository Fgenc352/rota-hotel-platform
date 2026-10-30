import { Component, computed, input, output, signal } from '@angular/core';
import type { Housekeeping, PhysicalRoom } from '../../models/pms.model';
import { housekeepingLabel } from '../../models/pms.model';
import { IconComponent } from '@shared/components/icon/icon.component';
@Component({
    selector: 'pms-room-board', standalone: true, imports: [IconComponent],
    templateUrl: './room-board.component.html', styleUrl: './room-board.component.css'
})
export class RoomBoardComponent {
    readonly rooms = input.required<PhysicalRoom[]>();
    readonly busy = input(false);
    readonly saved = output<{
        room: string;
        state: Housekeeping;
    }>();
    readonly opened = output<string>();
    readonly page = signal(0);
    readonly filter = signal('ALL');
    readonly query = signal('');
    readonly roomType = signal('ALL');
    readonly roomTypes = computed(() => [...new Map(this.rooms().map(room => [room.roomTypeId, { id: room.roomTypeId, name: room.roomTypeName }])).values()]);
    readonly choices = signal<Record<string, Housekeeping>>({});
    readonly housekeepingLabel = housekeepingLabel;
    readonly filtered = computed(() => this.rooms().filter(room => (!this.query().trim() || room.number.includes(this.query().trim()) || room.roomTypeName.toLocaleLowerCase('tr').includes(this.query().trim().toLocaleLowerCase('tr')))
        && (this.roomType() === 'ALL' || room.roomTypeId === this.roomType())
        && (this.filter() === 'ALL' || (this.filter() === 'OCCUPIED' ? !!room.occupantId : room.housekeeping === this.filter()))));
    readonly visible = computed(() => this.filtered().slice(this.page() * 12, this.page() * 12 + 12));
    search(value: string) {
        this.query.set(value);
        this.page.set(0);
    }
    scope(value: string) {
        this.filter.set(value);
        this.page.set(0);
    }
    type(value: string) {
        this.roomType.set(value);
        this.page.set(0);
    }
    pick(id: string, value: string) {
        this.choices.update(current => ({ ...current, [id]: value as Housekeeping }));
    }
    save(room: PhysicalRoom) {
        const state = this.choices()[room.id];
        if (!state || state === room.housekeeping)
            return;
        this.saved.emit({ room: room.id, state });
        this.choices.update(current => {
            const next = { ...current };
            delete next[room.id];
            return next;
        });
    }
}
