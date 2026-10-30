import { Component, input } from '@angular/core';
import type { ServiceOption } from '@booking/models/service-option.model';
import { serviceImage } from '@booking/presentation/service-visuals';
import { money } from '@shared/utils/currency';
@Component({
    selector: 'pms-service-catalog',
    standalone: true,
    templateUrl: './service-catalog.component.html',
    styleUrl: './service-catalog.component.css'
})
export class ServiceCatalogComponent {
    readonly services = input.required<ServiceOption[]>();
    readonly hotelKey = input.required<string>();
    readonly image = serviceImage;
    readonly money = money;
    unit(value: string) {
        return ({
            PER_STAY: 'konaklama başına', PER_NIGHT: 'gecelik', PER_PERSON_PER_NIGHT: 'kişi / gece'
        } as Record<string, string>)[value] || value;
    }
}
