import { Component, input, output } from '@angular/core';
import { HotelTimestampPipe } from '@shared/pipes/hotel-timestamp.pipe';
import { money } from '@shared/utils/currency';
import { serviceImage } from '@booking/presentation/service-visuals';
import type { ReservationServices } from '../../models/pms.model';
@Component({
    selector: 'pms-service-desk', standalone: true, imports: [HotelTimestampPipe],
    templateUrl: './service-desk.component.html', styleUrl: './service-desk.component.css'
})
export class ServiceDeskComponent {
    readonly plan = input.required<ReservationServices>();
    readonly hotelKey = input('');
    readonly closed = output<void>();
    readonly money = money;
    readonly image = serviceImage;
    progress(value: string) {
        return ({
            PENDING: 'Bekliyor', IN_PROGRESS: 'Hazırlanıyor', COMPLETED: 'Tamamlandı'
        } as Record<string, string>)[value] || value;
    }
    name(code: string) {
        return this.plan().services.find(task => task.line.code === code)?.line.name || code;
    }
    disabled() {
        return ['PENDING', 'CANCELLED', 'NO_SHOW'].includes(this.plan().status);
    }
}
