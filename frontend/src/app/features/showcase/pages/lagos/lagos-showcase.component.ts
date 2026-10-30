import { BookingScrollDirective } from '../../directives/booking-scroll.directive';
import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ShowcaseHotelStore } from '../../state/showcase-hotel.store';
import { RoomCollectionComponent } from '../../components/room-collection/room-collection.component';
import { BookingEmbedComponent } from '../../components/booking-embed/booking-embed.component';
@Component({
    standalone: true,
    imports: [
        BookingScrollDirective, RouterLink, RoomCollectionComponent, BookingEmbedComponent
    ],
    providers: [ShowcaseHotelStore],
    templateUrl: './lagos-showcase.component.html',
    styleUrl: './lagos-showcase.component.css'
})
export class LagosShowcaseComponent implements OnInit {
    readonly stay = inject(ShowcaseHotelStore);
    readonly year = new Date().getFullYear();
    ngOnInit() {
        window.scrollTo(0, 0);
        void this.stay.initialize('lagos-beach');
    }
    retry() {
        void this.stay.initialize('lagos-beach');
    }
}
