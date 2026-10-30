import { BookingRateBreakdownComponent } from '@booking/components/booking-rate-breakdown/booking-rate-breakdown.component';
import { BookingServicesComponent } from '../booking-services/booking-services.component';
import { Component, inject, input, output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { BookingPresentation } from '../../presentation/booking-presentation.service';
import type { Offer } from '../../models/offer.model';
@Component({
    selector: 'booking-guest-form',
    standalone: true,
    imports: [BookingRateBreakdownComponent, ReactiveFormsModule, BookingServicesComponent],
    styleUrl: './booking-guest-form.component.css',
    templateUrl: './booking-guest-form.component.html'
})
export class BookingGuestFormComponent {
    readonly presentation = inject(BookingPresentation);
    readonly t = this.presentation.t;
    readonly text = this.presentation.text;
    readonly money = this.presentation.money;
    readonly form = input.required<FormGroup>();
    readonly room = input.required<Offer>();
    readonly busy = input(false);
    readonly submitted = output<void>();
    readonly back = output<void>();
}
