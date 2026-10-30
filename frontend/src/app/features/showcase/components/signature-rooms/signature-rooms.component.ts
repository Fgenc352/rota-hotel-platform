import { RoomSelectorComponent } from '../room-selector/room-selector.component';
import { IconComponent } from '@shared/components/icon/icon.component';
import { Component, computed, input, output, signal } from '@angular/core';
import type { RoomDetails } from '@booking/models/room-details.model';
import { RoomDetailsDialogComponent } from '../room-details-dialog/room-details-dialog.component';
@Component({
    selector: 'signature-rooms',
    standalone: true,
    imports: [RoomSelectorComponent, IconComponent, RoomDetailsDialogComponent],
    templateUrl: './signature-rooms.component.html',
    styleUrl: './signature-rooms.component.css'
})
export class SignatureRoomsComponent {
    readonly rooms = input.required<RoomDetails[]>();
    readonly selected = output<string>();
    readonly active = signal(0);
    readonly photo = signal(0);
    readonly showDetails = signal(false);
    readonly room = computed(() => this.rooms()[this.active()] ?? this.rooms()[0]);
    readonly currency = new Intl.NumberFormat('tr-TR', {
        style: 'currency', currency: 'TRY', maximumFractionDigits: 2
    });
    choose(index: number) {
        this.active.set(index);
        this.photo.set(0);
    }
    movePhoto(direction: number) {
        const length = this.room()?.images.length ?? 0;
        if (length)
            this.photo.update(value => (value + direction + length) % length);
    }
}
