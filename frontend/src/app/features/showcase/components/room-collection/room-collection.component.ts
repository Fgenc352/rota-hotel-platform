import { BookingRateBreakdownComponent } from '@booking/components/booking-rate-breakdown/booking-rate-breakdown.component';
import { catalogText } from '@booking/presentation/catalog-translations';
import { IconComponent } from '@shared/components/icon/icon.component';
import { Component, input, output, signal } from '@angular/core';
import type { RoomDetails } from '@booking/models/room-details.model';
import type { Offer } from '@booking/models/offer.model';
import type { BookingCurrency, BookingLanguage } from '@booking/models/booking-preferences.model';
import { RoomDetailsDialogComponent } from '../room-details-dialog/room-details-dialog.component';
@Component({
    selector: 'showcase-room-collection',
    standalone: true,
    imports: [BookingRateBreakdownComponent, IconComponent, RoomDetailsDialogComponent],
    templateUrl: './room-collection.component.html',
    styleUrl: './room-collection.component.css'
})
export class RoomCollectionComponent {
    readonly rooms = input.required<RoomDetails[]>();
    readonly offers = input<Offer[] | null>(null);
    readonly currency = input<BookingCurrency>('TRY');
    readonly language = input<BookingLanguage>('tr');
    readonly eurRate = input(1);
    readonly layout = input<'grid' | 'list'>('grid');
    readonly busy = input(false);
    readonly selected = output<string>();
    readonly detail = signal<RoomDetails | null>(null);
    readonly text = (value: string) => catalogText(value, this.language());
    readonly t = (tr: string, en: string) => this.language() === 'tr' ? tr : en;
    offer(id: string) {
        return this.offers()?.find(offer => offer.roomTypeId === id);
    }
    readonly price = (value: number) => {
        return new Intl.NumberFormat(this.language() === 'tr' ? 'tr-TR' : 'en-GB', {
            style: 'currency', currency: this.currency(), minimumFractionDigits: 0, maximumFractionDigits: 2
        }).format(this.currency() === 'EUR' ? value / this.eurRate() : value);
    };
}
