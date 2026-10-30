import { Component, computed, input } from '@angular/core';
import type { Offer } from '../../models/offer.model';
import { groupedStayRates, weekendRateSummary } from '../../presentation/weekend-rate-summary';
@Component({
    selector: 'booking-rate-breakdown',
    standalone: true,
    templateUrl: './booking-rate-breakdown.component.html',
    styleUrl: './booking-rate-breakdown.component.css'
})
export class BookingRateBreakdownComponent {
    readonly offer = input.required<Offer>();
    readonly language = input('tr');
    readonly formatMoney = input.required<(value: number) => string>();
    readonly weekend = computed(() => weekendRateSummary(this.offer()));
    readonly t = (tr: string, en: string) => this.language() === 'tr' ? tr : en;
    readonly groups = computed(() => groupedStayRates(this.offer()));
}
