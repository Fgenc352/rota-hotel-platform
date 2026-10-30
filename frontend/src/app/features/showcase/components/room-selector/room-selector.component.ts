import { Component, ElementRef, input, output, viewChildren } from '@angular/core';
import type { RoomDetails } from '@booking/models/room-details.model';
import { IconComponent } from '@shared/components/icon/icon.component';
export type RoomSelectorVariant = 'quiet' | 'maison' | 'signature' | 'atelier';
@Component({
    selector: 'showcase-room-selector',
    standalone: true,
    imports: [IconComponent],
    templateUrl: './room-selector.component.html',
    styleUrl: './room-selector.component.css'
})
export class RoomSelectorComponent {
    readonly rooms = input.required<RoomDetails[]>();
    readonly active = input(0);
    readonly variant = input<RoomSelectorVariant>('quiet');
    readonly changed = output<number>();
    private readonly tabs = viewChildren<ElementRef<HTMLButtonElement>>('tab');
    navigate(event: Event, key: 'ArrowRight' | 'ArrowLeft' | 'Home' | 'End') {
        const count = this.rooms().length;
        if (!count)
            return;
        const next = key === 'Home' ? 0 : key === 'End' ? count - 1
            : (this.active() + (key === 'ArrowRight' ? 1 : -1) + count) % count;
        event.preventDefault();
        this.changed.emit(next);
        this.tabs()[next]?.nativeElement.focus();
    }
}
