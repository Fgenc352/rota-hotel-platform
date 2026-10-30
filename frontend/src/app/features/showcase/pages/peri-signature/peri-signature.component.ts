import { BookingScrollDirective } from '../../directives/booking-scroll.directive';
import { IconComponent } from '@shared/components/icon/icon.component';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
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
    templateUrl: './peri-signature.component.html',
    styleUrls: ['../../styles/signature-shared.css', './peri-signature.component.css']
})
export class PeriSignatureComponent implements OnInit {
    readonly stay = inject(ShowcaseHotelStore);
    readonly money = money;
    readonly minimumPrice = computed(() => this.stay.rooms().length
        ? Math.min(...this.stay.rooms().map(room => room.nightlyPrice)) : null);
    readonly year = new Date().getFullYear();
    readonly scene = signal(0);
    readonly scenes = [
        {
            image: '/images/showcase/peri-cave-reverse.webp', title: 'Taşın hafızası', caption: 'Doğal dokular, kendine özgü bir karakter.'
        },
        {
            image: '/images/showcase/peri-terrace-living.webp', title: 'Işığın peşinde', caption: 'Günün değişen renklerine açılan bir pencere.'
        },
        {
            image: '/images/showcase/peri-cave-evening.webp', title: 'Sessizliğe dönüş', caption: 'Geriye sadece anın kaldığı bir akşam.'
        }
    ];
    ngOnInit() {
        window.scrollTo(0, 0);
        void this.stay.initialize('peri-stone');
    }
    retry() {
        void this.stay.initialize('peri-stone');
    }
}
