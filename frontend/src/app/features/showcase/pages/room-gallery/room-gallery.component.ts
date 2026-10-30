import { Component, inject, signal, ViewEncapsulation } from '@angular/core';
import { BookingApi } from '@booking/data-access/booking-api.service';
import type { RoomDetails } from '@booking/models/room-details.model';
import { RoomDetailsDialogComponent } from '../../components/room-details-dialog/room-details-dialog.component';
@Component({
    standalone: true, imports: [RoomDetailsDialogComponent], encapsulation: ViewEncapsulation.None,
    templateUrl: './room-gallery.component.html', styleUrl: './room-gallery.component.css'
})
export class RoomGalleryComponent {
    private readonly api = inject(BookingApi);
    private readonly query = new URLSearchParams(location.search);
    readonly hotel = this.query.get('hotel') || '';
    readonly roomId = this.query.get('room') || '';
    readonly currency = this.query.get('currency') === 'EUR' ? 'EUR' : 'TRY';
    readonly language = this.query.get('lang') === 'en' ? 'en' : 'tr';
    readonly selectable = this.query.get('selectable') === 'true';
    readonly room = signal<RoomDetails | null>(null);
    readonly eurRate = signal(1);
    readonly error = signal('');
    async ngOnInit() {
        try {
            if (!['lagos-beach', 'peri-stone'].includes(this.hotel))
                throw new Error('Otel bulunamadı.');
            const [rooms, hotel] = await Promise.all([this.api.rooms(this.hotel), this.api.hotel(this.hotel)]);
            const room = rooms.find(item => item.id === this.roomId);
            if (!room)
                throw new Error('Oda bulunamadı.');
            this.eurRate.set(hotel.eurRate);
            this.room.set(room);
        }
        catch {
            this.error.set(this.language === 'tr' ? 'Oda görselleri yüklenemedi. Kapatıp tekrar deneyebilirsiniz.' : 'Room photos could not be loaded. Close and try again.');
        }
    }
    finish(selected: boolean) {
        if (window.parent === window) {
            location.assign('/signature/' + (this.hotel === 'peri-stone' ? 'peri-stone' : 'lagos-beach'));
            return;
        }
        window.parent.postMessage({ type: selected ? 'rota:gallery:select' : 'rota:gallery:close', roomId: this.roomId }, '*');
    }
}
