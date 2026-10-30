import { Component, effect, inject, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { BookingStore } from '../../state/booking.store';
import { BookingPresentation } from '../../presentation/booking-presentation.service';
import { StaySearchComponent } from '../../components/stay-search/stay-search.component';
import { BookingGuestFormComponent } from '../../components/booking-guest-form/booking-guest-form.component';
import { RoomOffersComponent } from '../../components/room-offers/room-offers.component';
import { BookingConfirmationComponent } from '../../components/booking-confirmation/booking-confirmation.component';
@Component({
    standalone: true,
    imports: [
        StaySearchComponent,
        BookingGuestFormComponent,
        RoomOffersComponent,
        BookingConfirmationComponent
    ],
    providers: [BookingPresentation, BookingStore],
    styleUrl: './booking-page.component.css',
    templateUrl: './booking-page.component.html'
})
export class BookingPageComponent implements OnInit {
    readonly booking = inject(BookingStore);
    private readonly title = inject(Title);
    constructor() {
        effect(() => this.title.setTitle((this.booking.hotel()?.name || 'Rota') + ' | Rezervasyon'));
    }
    ngOnInit() {
        void this.booking.initialize();
    }
}
