import { Injectable, inject } from '@angular/core';
import type { SegmentInput } from '../models/segment-input.model';
import { HttpApi } from '@core/http/http-api.service';
import type { Segment } from '../models/segment.model';
@Injectable({ providedIn: 'root' })
export class SegmentApi {
    private readonly http = inject(HttpApi);
    list() {
        return this.http.request<Segment[]>('/api/crm/segments');
    }
    create(body: SegmentInput) {
        return this.http.request<{
            id: string;
        }>('/api/crm/segments', 'POST', body);
    }
    delete(id: string) {
        return this.http.request('/api/crm/segments/' + id, 'DELETE');
    }
}
