import { Injectable, inject } from '@angular/core';
import { HttpApi } from '@core/http/http-api.service';
import type { SortSelection } from '@shared/models/sort.model';
import type { GuestSearch } from '../models/guest-search.model';
import type { Guest } from '../models/guest.model';
import type { GuestDetail } from '../models/guest-detail.model';
import type { Page } from '@shared/models/page.model';
@Injectable({ providedIn: 'root' })
export class GuestApi {
    private readonly http = inject(HttpApi);
    search(q: string, page: number, segment: string, filters?: GuestSearch, sort: SortSelection = { field: 'LAST_NAME', direction: 'ASC' }) {
        const query = new URLSearchParams({
            q,
            page: String(page),
            size: '20',
            segment,
            sort: sort.field,
            direction: sort.direction
        });
        if (filters) {
            for (const [key, value] of Object.entries(filters)) {
                if (value !== '')
                    query.set(key, String(value));
            }
        }
        return this.http.request<Page<Guest>>('/api/crm/guests?' + query);
    }
    detail(id: string) {
        return this.http.request<GuestDetail>('/api/crm/guests/' + id);
    }
    addNote(id: string, body: string) {
        return this.http.request('/api/crm/guests/' + id + '/notes', 'POST', { body });
    }
    deleteNote(id: string, noteId: string) {
        return this.http.request(`/api/crm/guests/${id}/notes/${noteId}`, 'DELETE');
    }
    addTag(id: string, tag: string) {
        return this.http.request('/api/crm/guests/' + id + '/tags', 'POST', { tag });
    }
    removeTag(id: string, tag: string) {
        return this.http.request('/api/crm/guests/' + id + '/tags/' + encodeURIComponent(tag), 'DELETE');
    }
}
