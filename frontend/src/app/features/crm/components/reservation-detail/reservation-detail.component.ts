import { RouterLink } from '@angular/router';
import { HotelTimestampPipe } from '@shared/pipes/hotel-timestamp.pipe';
import { Component, ElementRef, afterNextRender, input, output, viewChild, OnDestroy } from '@angular/core';
import type { Booking } from '../../models/booking.model';
import { allowed, label } from '../../utils/reservation-status';
import { money } from '@shared/utils/currency';
import { StayDatesComponent } from '@shared/components/stay-dates/stay-dates.component';
import { IconComponent } from '@shared/components/icon/icon.component';
import { stayNights } from '@shared/utils/display-date';
@Component({
    selector: 'crm-reservation-detail', standalone: true, imports: [
        StayDatesComponent, IconComponent, RouterLink, HotelTimestampPipe
    ],
    templateUrl: './reservation-detail.component.html', styleUrl: './reservation-detail.component.css'
})
export class ReservationDetailComponent implements OnDestroy {
    readonly booking = input.required<Booking>();
    readonly busy = input(false);
    readonly error = input('');
    readonly closed = output<void>();
    readonly changed = output<string>();
    readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
    readonly money = money;
    readonly label = label;
    readonly allowed = allowed;
    readonly nights = stayNights;
    readonly actions: Record<string, string> = {
        CONFIRMED: 'Rezervasyonu onayla', CHECKED_IN: 'Misafirin girişini yap',
        CHECKED_OUT: 'Çıkışı tamamla', CANCELLED: 'Rezervasyonu iptal et', NO_SHOW: 'Gelmedi olarak işaretle'
    };
    constructor() {
        afterNextRender(() => this.dialog().nativeElement.showModal());
    }
    ngOnDestroy() {
        this.dialog().nativeElement.close();
    }
}
