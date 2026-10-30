import { BookingScrollDirective } from '../../directives/booking-scroll.directive';
import { IconComponent } from '@shared/components/icon/icon.component';
import { Component, computed, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { money } from '@shared/utils/currency';
import { ShowcaseHotelStore } from '../../state/showcase-hotel.store';
import { SignatureMotionDirective } from '../../components/signature-motion/signature-motion.directive';
import { SignatureRoomsComponent } from '../../components/signature-rooms/signature-rooms.component';
import { BookingEmbedComponent } from '../../components/booking-embed/booking-embed.component';
@Component({
    standalone: true,
    imports: [
        BookingScrollDirective, IconComponent, RouterLink, SignatureMotionDirective, SignatureRoomsComponent, BookingEmbedComponent
    ],
    providers: [ShowcaseHotelStore],
    templateUrl: './lagos-signature.component.html',
    styleUrls: ['../../styles/signature-shared.css', './lagos-signature.component.css']
})
export class LagosSignatureComponent implements OnInit {
    readonly stay = inject(ShowcaseHotelStore);
    readonly money = money;
    readonly minimumPrice = computed(() => this.stay.rooms().length
        ? Math.min(...this.stay.rooms().map(room => room.nightlyPrice)) : null);
    readonly year = new Date().getFullYear();
    ngOnInit() {
        window.scrollTo(0, 0);
        void this.stay.initialize('lagos-beach');
    }
    retry() {
        void this.stay.initialize('lagos-beach');
    }
}
