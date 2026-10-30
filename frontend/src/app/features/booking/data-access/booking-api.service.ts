import { Injectable, inject } from '@angular/core';
import type { StayInput } from '../models/stay-input.model';
import type { BookingInput } from '../models/booking-input.model';
import { HttpApi } from '@core/http/http-api.service';
import type { RoomDetails } from '../models/room-details.model';
import type { Hotel } from '../models/hotel.model';
import type { Offer } from '../models/offer.model';
import type { Receipt } from '../models/receipt.model';
@Injectable({ providedIn: 'root' })
export class BookingApi {
    private readonly http = inject(HttpApi);
    private path(key: string) {
        return '/api/public/hotels/' + encodeURIComponent(key);
    }
    hotel(key: string) {
        return this.http.request<Hotel>(this.path(key));
    }
    rooms(key: string) {
        return this.http.request<RoomDetails[]>(this.path(key) + '/rooms');
    }
    services(key: string) {
        return this.http.request<import('../models/service-option.model').ServiceOption[]>(this.path(key) + '/services');
    }
    availability(key: string, stay: StayInput) {
        const query = new URLSearchParams(Object.entries(stay).map(([key, value]) => [key, String(value)]));
        return this.http.request<Offer[]>(this.path(key) + '/availability?' + query);
    }
    book(key: string, body: BookingInput, idempotency: string) {
        return this.http.request<Receipt>(this.path(key) + '/reservations', 'POST', body, { 'Idempotency-Key': idempotency });
    }
    track(key: string, sessionId: string, stage: string) {
        return this.http.request(this.path(key) + '/funnel', 'POST', {
            stage,
            sessionId
        });
    }
}
