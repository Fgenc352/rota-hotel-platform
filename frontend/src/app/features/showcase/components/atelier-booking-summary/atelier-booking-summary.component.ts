import { serviceText } from '@booking/presentation/catalog-translations';
import { Component, computed, inject, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { BookingExperience } from '../../state/booking-experience.service';
import { StayDatesComponent } from '@shared/components/stay-dates/stay-dates.component';
import { IconComponent } from '@shared/components/icon/icon.component';
@Component({
    selector: 'atelier-booking-summary', standalone: true, imports: [StayDatesComponent, IconComponent],
    templateUrl: './atelier-booking-summary.component.html', styleUrl: './atelier-booking-summary.component.css'
})
export class AtelierBookingSummaryComponent {
    readonly experience = inject(BookingExperience);
    readonly booking = this.experience.booking;
    readonly serviceLabel = (code: string, name: string) => serviceText(code, 'name', name, this.booking.lang());
    readonly extras = computed(() => this.booking.serviceLines().filter(line => !line.included));
    readonly gallery = output<void>();
    readonly stay = toSignal(this.booking.searchForm.valueChanges, { initialValue: this.booking.searchForm.getRawValue() });
}
