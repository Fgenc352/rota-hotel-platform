import { Injectable, inject } from '@angular/core';
import { HttpApi } from '@core/http/http-api.service';
import type { Page } from '@shared/models/page.model';
import type { SortSelection } from '@shared/models/sort.model';
import type { ReservationSearch } from '../models/reservation-search.model';
import type { Booking } from '../models/booking.model';
@Injectable({ providedIn: 'root' })
export class ReservationApi {
    private readonly http = inject(HttpApi);
    list(page: number, sort: SortSelection = { field: 'CREATED_AT', direction: 'DESC' }, filters: ReservationSearch = {}) {
        const query = new URLSearchParams({
            page: String(page), sort: sort.field, direction: sort.direction
        });
        for (const [key, value] of Object.entries(filters)) {
            if (value !== undefined && value !== null && String(value).trim() !== '')
                query.set(key, String(value).trim());
        }
        return this.http.request<Page<Booking>>('/api/crm/reservations?' + query);
    }
    detail(id: string) {
        return this.http.request<Booking>('/api/crm/reservations/' + encodeURIComponent(id));
    }
    changeStatus(id: string, status: string) {
        return this.http.request('/api/crm/reservations/' + id + '/status', 'POST', { status });
    }
}
