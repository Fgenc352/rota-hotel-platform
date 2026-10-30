import { Component, input } from '@angular/core';
import { formatCalendarDate, stayNights } from '../../utils/display-date';
@Component({
    selector: 'ui-stay-dates', standalone: true,
    templateUrl: './stay-dates.component.html', styleUrl: './stay-dates.component.css'
})
export class StayDatesComponent {
    readonly checkIn = input<string | undefined>('');
    readonly checkOut = input<string | undefined>('');
    readonly compact = input(false);
    readonly locale = input('tr-TR');
    readonly format = formatCalendarDate;
    readonly nights = stayNights;
}
