import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ShowcaseHotelStore } from '../../state/showcase-hotel.store';
import { collectionHotels, type CollectionHotelKey } from '../../presentation/collection-hotel-content';
import { CollectionHeaderComponent } from '../../components/collection-header/collection-header.component';
import { CollectionFooterComponent } from '../../components/collection-footer/collection-footer.component';
import { AtelierRoomsComponent } from '../../components/atelier-rooms/atelier-rooms.component';
import { BookingEmbedComponent } from '../../components/booking-embed/booking-embed.component';
import { BookingScrollDirective } from '../../directives/booking-scroll.directive';
import { SignatureMotionDirective } from '../../components/signature-motion/signature-motion.directive';
import { IconComponent } from '@shared/components/icon/icon.component';
@Component({
    standalone: true,
    imports: [
        RouterLink, CollectionHeaderComponent, CollectionFooterComponent, AtelierRoomsComponent, BookingEmbedComponent, BookingScrollDirective, SignatureMotionDirective, IconComponent
    ],
    providers: [ShowcaseHotelStore],
    templateUrl: './quiet-hotel.component.html',
    styleUrls: ['../../styles/collection-shared.css', './quiet-hotel.component.css']
})
export class QuietHotelComponent {
    readonly stay = inject(ShowcaseHotelStore);
    readonly key = inject(ActivatedRoute).snapshot.data['hotel'] as CollectionHotelKey;
    readonly content = collectionHotels[this.key];
    ngOnInit() {
        window.scrollTo({ top: 0, behavior: 'instant' });
        this.retry();
    }
    retry() {
        void this.stay.initialize(this.key);
    }
}
