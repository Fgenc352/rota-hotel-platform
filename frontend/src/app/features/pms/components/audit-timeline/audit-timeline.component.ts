import { Component, input } from '@angular/core';
import { HotelTimestampPipe } from '@shared/pipes/hotel-timestamp.pipe';
import { label } from '@crm/utils/reservation-status';
import type { RoomAudit } from '../../models/pms.model';
import { housekeepingLabel } from '../../models/pms.model';
@Component({
    selector: 'pms-audit-timeline', standalone: true, imports: [HotelTimestampPipe],
    templateUrl: './audit-timeline.component.html', styleUrl: './audit-timeline.component.css'
})
export class AuditTimelineComponent {
    readonly history = input.required<RoomAudit[]>();
    title(action: string) {
        return ({
            ROOM_ASSIGNED: 'Oda ataması', HOUSEKEEPING_CHANGED: 'Temizlik durumu', STAY_STATUS_CHANGED: 'Konaklama durumu', ROOM_VACATED: 'Oda boşaltıldı'
        } as Record<string, string>)[action] || action;
    }
    state(json: string) {
        try {
            const value = JSON.parse(json);
            return [value.room ? 'Oda ' + value.room : '', value.status ? label(value.status) : '', value.housekeeping ? housekeepingLabel(value.housekeeping) : ''].filter(Boolean).join(' · ') || '—';
        }
        catch {
            return '—';
        }
    }
}
