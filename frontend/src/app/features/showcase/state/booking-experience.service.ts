import { Injectable, Injector, afterNextRender, computed, effect, inject, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { BookingStore } from '@booking/state/booking.store';
import { BookingApi } from '@booking/data-access/booking-api.service';
import type { RoomDetails } from '@booking/models/room-details.model';
import { BookingNavigation } from './booking-navigation.service';
@Injectable()
export class BookingExperience {
    readonly booking = inject(BookingStore);
    readonly journey = inject(BookingNavigation);
    private readonly api = inject(BookingApi);
    private readonly title = inject(Title);
    private readonly injector = inject(Injector);
    readonly rooms = signal<RoomDetails[]>([]);
    readonly catalogError = signal('');
    readonly cave = this.booking.key === 'peri-stone';
    readonly requestedRoom = new URLSearchParams(location.search).get('room');
    readonly preferred = computed(() => this.rooms().find(room => room.id === this.requestedRoom));
    readonly preferredOffer = (offer: {
        roomTypeId: string;
    }) => offer.roomTypeId === this.requestedRoom;
    readonly summaryOffer = computed(() => this.booking.selected() ?? this.journey.rememberedRoom() ?? null);
    readonly summaryRoom = computed(() => this.rooms().find(room => room.id === this.summaryOffer()?.roomTypeId));
    readonly selectedPhoto = computed(() => this.summaryRoom()?.images[0]
        || '/images/showcase/' + (this.cave ? 'peri' : 'lagos') + '-room.webp');
    readonly availableRooms = computed(() => this.booking.offers().map(offer => this.rooms().find(room => room.id === offer.roomTypeId) ?? {
        id: offer.roomTypeId, name: offer.name, description: '', capacity: offer.capacity, areaSquareMeters: 0,
        bedDescription: '', nightlyPrice: offer.nightlyPrice, currency: offer.currency, minNights: offer.minNights,
        amenities: [], images: ['/images/showcase/' + (this.cave ? 'peri' : 'lagos') + '-room.webp']
    }));
    constructor() {
        effect(() => this.title.setTitle((this.booking.hotel()?.name || 'Rota') + ' | Konaklamanızı planlayın'));
    }
    initialize() {
        void this.booking.initialize();
        void this.loadRooms();
    }
    async loadRooms() {
        this.catalogError.set('');
        try {
            this.rooms.set(await this.api.rooms(this.booking.key));
        }
        catch {
            this.catalogError.set(this.booking.t('Oda detayları yüklenemedi. Müsaitlik aramasına devam edebilir veya tekrar deneyebilirsiniz.', 'Room details could not be loaded. You can still search availability or retry.'));
        }
    }
    async search() {
        const ready = await this.journey.search();
        if (ready && this.requestedRoom && this.booking.offers().some(this.preferredOffer)) {
            this.select(this.requestedRoom);
        }
    }
    select(id: string) {
        if (!this.journey.select(id))
            return;
        afterNextRender(() => {
            document.querySelector<HTMLInputElement>('booking-guest-form input[autocomplete="given-name"]')?.focus();
        }, { injector: this.injector });
    }
}
