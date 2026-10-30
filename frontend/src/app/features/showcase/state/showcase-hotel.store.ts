import { scrollToBookingWidget } from '@shared/utils/booking-widget-scroll';
import { Injectable, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { BookingApi } from '@booking/data-access/booking-api.service';
import type { Hotel } from '@booking/models/hotel.model';
import type { RoomDetails } from '@booking/models/room-details.model';
@Injectable()
export class ShowcaseHotelStore {
    private readonly api = inject(BookingApi);
    private readonly title = inject(Title);
    readonly hotel = signal<Hotel | null>(null);
    readonly rooms = signal<RoomDetails[]>([]);
    readonly busy = signal(true);
    readonly error = signal('');
    readonly preferredRoom = signal<string | null>(null);
    async initialize(key: string) {
        this.busy.set(true);
        this.error.set('');
        try {
            const [hotel, rooms] = await Promise.all([this.api.hotel(key), this.api.rooms(key)]);
            this.hotel.set(hotel);
            this.rooms.set(rooms);
            this.title.setTitle(hotel.name + ' | Kendinize bir yolculuk');
        }
        catch (error) {
            this.error.set(error instanceof Error ? error.message : 'Otel bilgileri yüklenemedi.');
        }
        finally {
            this.busy.set(false);
        }
    }
    chooseRoom(id: string) {
        this.preferredRoom.set(id);
        requestAnimationFrame(() => scrollToBookingWidget(document, window));
    }
}
