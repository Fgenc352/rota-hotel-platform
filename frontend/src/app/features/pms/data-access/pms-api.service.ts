import { dailyServiceRequest } from './daily-service-request';
import type { DailyOperations, DailyTask, DailyAudit } from '../models/daily-operations.model';
import { Injectable, inject } from '@angular/core';
import { HttpApi } from '@core/http/http-api.service';
import type { Housekeeping, RoomAuditPage, PmsBoard, ReservationServices } from '../models/pms.model';
@Injectable({ providedIn: 'root' })
export class PmsApi {
    private readonly http = inject(HttpApi);
    daily(date: string) {
        return this.http.request<DailyOperations>('/api/crm/pms/daily' + (date ? '?date=' + encodeURIComponent(date) : ''));
    }
    dailyUpdate(task: DailyTask) {
        return this.http.request('/api/crm/pms/reservations/' + encodeURIComponent(task.reservationId) + '/daily-services/' + encodeURIComponent(task.code), 'POST', dailyServiceRequest(task));
    }
    dailyHistory(task: DailyTask) {
        return this.http.request<DailyAudit[]>('/api/crm/pms/reservations/' + encodeURIComponent(task.reservationId) + '/daily-services/' + encodeURIComponent(task.code) + '/history?date=' + encodeURIComponent(task.date));
    }
    board() {
        return this.http.request<PmsBoard>('/api/crm/pms/board');
    }
    history(page: number) {
        return this.http.request<RoomAuditPage>('/api/crm/pms/history?page=' + page + '&size=20');
    }
    assign(reservation: string, room: string) {
        return this.http.request('/api/crm/pms/reservations/' + encodeURIComponent(reservation) + '/room', 'POST', { roomId: room });
    }
    services(reservation: string) {
        return this.http.request<ReservationServices>('/api/crm/pms/reservations/' + encodeURIComponent(reservation) + '/services');
    }
    housekeeping(room: string, state: Housekeeping) {
        return this.http.request('/api/crm/pms/rooms/' + encodeURIComponent(room) + '/housekeeping', 'POST', { state });
    }
}
