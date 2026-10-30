import { serviceText } from '../../presentation/catalog-translations';
import { Component, inject } from '@angular/core';
import { IconComponent } from '@shared/components/icon/icon.component';
import { serviceImage } from '../../presentation/service-visuals';
import { BookingStore } from '../../state/booking.store';
@Component({
    selector: 'booking-services',
    standalone: true,
    imports: [IconComponent],
    templateUrl: './booking-services.component.html',
    styleUrl: './booking-services.component.css'
})
export class BookingServicesComponent {
    readonly booking = inject(BookingStore);
    readonly name = (service: {
        code: string;
        name: string;
    }) => serviceText(service.code, 'name', service.name, this.booking.lang());
    readonly description = (service: {
        code: string;
        description: string;
    }) => serviceText(service.code, 'description', service.description, this.booking.lang());
    readonly photo = (code: string) => serviceImage(this.booking.key, code);
    readonly selected = (code: string) => this.booking.selectedServices().includes(code);
    readonly lineTotal = (code: string) => this.booking.serviceLines().find(line => line.code === code)?.total || 0;
    readonly units = {
        PER_STAY: 'rezervasyon başına', PER_NIGHT: 'gece başına', PER_PERSON_PER_NIGHT: 'kişi / gece'
    };
    readonly englishUnits = {
        PER_STAY: 'per stay', PER_NIGHT: 'per night', PER_PERSON_PER_NIGHT: 'per person / night'
    };
    toggle(code: string, event: Event) {
        this.booking.toggleService(code, (event.target as HTMLInputElement).checked);
    }
}
