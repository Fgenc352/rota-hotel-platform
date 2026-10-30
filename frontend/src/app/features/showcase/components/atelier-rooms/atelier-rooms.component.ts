import { RoomSelectorComponent, type RoomSelectorVariant } from '../room-selector/room-selector.component';
import { Component, computed, input, output, signal } from '@angular/core';
import type { RoomDetails } from '@booking/models/room-details.model';
import { money } from '@shared/utils/currency';
import { IconComponent } from '@shared/components/icon/icon.component';
import { RoomDetailsDialogComponent } from '../room-details-dialog/room-details-dialog.component';
@Component({
    selector: 'atelier-rooms', standalone: true, imports: [RoomSelectorComponent, IconComponent, RoomDetailsDialogComponent],
    templateUrl: './atelier-rooms.component.html', styleUrl: './atelier-rooms.component.css'
})
export class AtelierRoomsComponent {
    readonly rooms = input.required<RoomDetails[]>();
    readonly selectorVariant = input<RoomSelectorVariant>('atelier');
    readonly selected = output<string>();
    readonly active = signal(0);
    readonly photo = signal(0);
    readonly gallery = signal(false);
    readonly current = computed(() => this.rooms()[this.active()] || this.rooms()[0]);
    readonly money = money;
    choose(index: number) {
        this.active.set(index);
        this.photo.set(0);
    }
    move(direction: number) {
        const images = this.current()?.images || [];
        if (images.length)
            this.photo.update(index => (index + direction + images.length) % images.length);
    }
}
