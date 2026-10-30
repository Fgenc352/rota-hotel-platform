import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { money } from '@shared/utils/currency';
import { IconComponent } from '@shared/components/icon/icon.component';
import { ShowcaseHotelStore } from '../../state/showcase-hotel.store';
import { SignatureMotionDirective } from '../../components/signature-motion/signature-motion.directive';
import { BookingScrollDirective } from '../../directives/booking-scroll.directive';
import { AtelierHeaderComponent } from '../../components/atelier-header/atelier-header.component';
import { AtelierFooterComponent } from '../../components/atelier-footer/atelier-footer.component';
import { AtelierRoomsComponent } from '../../components/atelier-rooms/atelier-rooms.component';
import { BookingEmbedComponent } from '../../components/booking-embed/booking-embed.component';
@Component({
    standalone: true,
    imports: [
        RouterLink, IconComponent, SignatureMotionDirective, BookingScrollDirective, AtelierHeaderComponent, AtelierFooterComponent, AtelierRoomsComponent, BookingEmbedComponent
    ],
    providers: [ShowcaseHotelStore],
    templateUrl: './peri-atelier.component.html',
    styleUrls: ['../../styles/atelier-shared.css', './peri-atelier.component.css']
})
export class PeriAtelierComponent {
    readonly stay = inject(ShowcaseHotelStore);
    readonly money = money;
    readonly minimumPrice = computed(() => this.stay.rooms().length ? Math.min(...this.stay.rooms().map(room => room.nightlyPrice)) : null);
    ngOnInit() {
        window.scrollTo({ top: 0, behavior: 'instant' });
        void this.stay.initialize('peri-stone');
    }
    retry() {
        void this.stay.initialize('peri-stone');
    }
}
