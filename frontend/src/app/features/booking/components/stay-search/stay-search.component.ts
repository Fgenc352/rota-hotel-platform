import { Component, inject, input, output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { BookingPresentation } from '../../presentation/booking-presentation.service';
@Component({
    selector: 'booking-stay-search',
    standalone: true,
    imports: [ReactiveFormsModule],
    styleUrl: './stay-search.component.css',
    templateUrl: './stay-search.component.html'
})
export class StaySearchComponent {
    private readonly presentation = inject(BookingPresentation);
    readonly t = this.presentation.t;
    readonly money = this.presentation.money;
    readonly form = input.required<FormGroup>();
    readonly busy = input(false);
    readonly hotelLoaded = input(false);
    readonly submitted = output<void>();
}
