import { Injectable, inject } from '@angular/core';
import { HttpApi } from '@core/http/http-api.service';
import type { Analytics } from '../models/analytics.model';
import type { FailedEvent } from '../models/failed-event.model';
@Injectable({ providedIn: 'root' })
export class OperationsApi {
    private readonly http = inject(HttpApi);
    analytics() {
        return this.http.request<Analytics>('/api/crm/analytics');
    }
    failed() {
        return this.http.request<FailedEvent[]>('/api/crm/events/failed');
    }
    retry(id: string) {
        return this.http.request('/api/crm/events/' + id + '/retry', 'POST');
    }
}
