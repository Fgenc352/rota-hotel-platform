import { BookingRateBreakdownComponent } from '@booking/components/booking-rate-breakdown/booking-rate-breakdown.component';
import { Component, inject, input, output } from '@angular/core';
import { BookingPresentation } from '../../presentation/booking-presentation.service';
import type { Offer } from '../../models/offer.model';
@Component({
    selector: 'booking-room-offers',
    standalone: true,
    imports: [BookingRateBreakdownComponent],
    styleUrl: './room-offers.component.css',
    templateUrl: './room-offers.component.html'
})
export class RoomOffersComponent {
    readonly presentation = inject(BookingPresentation);
    readonly t = this.presentation.t;
    readonly text = this.presentation.text;
    readonly money = this.presentation.money;
    readonly offers = input.required<Offer[]>();
    readonly selected = output<Offer>();
}
