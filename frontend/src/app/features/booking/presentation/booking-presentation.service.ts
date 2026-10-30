import { catalogText } from './catalog-translations';
import { Injectable, signal } from '@angular/core';
import type { BookingLanguage, BookingCurrency } from '../models/booking-preferences.model';
import type { Hotel } from '../models/hotel.model';
@Injectable()
export class BookingPresentation {
    readonly lang = signal<BookingLanguage>('tr');
    readonly currency = signal<BookingCurrency>('TRY');
    readonly hotel = signal<Hotel | null>(null);
    readonly t = (tr: string, en: string) => this.lang() === 'tr' ? tr : en;
    readonly text = (value: string) => catalogText(value, this.lang());
    readonly money = (value: number) => new Intl.NumberFormat(this.lang() === 'tr' ? 'tr-TR' : 'en-GB', {
        style: 'currency',
        currency: this.currency()
    }).format(this.currency() === 'EUR' ? value / (this.hotel()?.eurRate || 1) : value);
}
