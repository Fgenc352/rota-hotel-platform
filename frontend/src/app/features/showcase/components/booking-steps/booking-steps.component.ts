import { Component, inject } from '@angular/core';
import { BookingNavigation } from '../../state/booking-navigation.service';
import type { BookingStep } from '../../state/booking-navigation-policy';
@Component({
    selector: 'showcase-booking-steps',
    standalone: true,
    templateUrl: './booking-steps.component.html',
    styleUrl: './booking-steps.component.css'
})
export class BookingStepsComponent {
    readonly journey = inject(BookingNavigation);
    readonly booking = this.journey.booking;
    readonly steps: {
        id: BookingStep;
        tr: string;
        en: string;
    }[] = [
        {
            id: 1, tr: 'Tarihleriniz', en: 'Your dates'
        },
        {
            id: 2, tr: 'Odanız', en: 'Your room'
        },
        {
            id: 3, tr: 'Bilgileriniz', en: 'Your details'
        },
        {
            id: 4, tr: 'Hazırsınız', en: 'All set'
        }
    ];
}
