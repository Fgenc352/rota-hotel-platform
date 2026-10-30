import type { Offer } from '../models/offer.model';
/** Uses the server tariff and rounded nightly quotes; never reprices the booking. */
export function weekendRateSummary(offer: Offer) {
    const base = offer.baseNightlyPrice;
    const multiplier = offer.weekendMultiplier;
    if (base == null || multiplier == null || multiplier <= 1)
        return null;
    const nights = offer.nights.filter(night => {
        const day = new Date(night.date + 'T00:00:00Z').getUTCDay();
        return (day === 0 || day === 6) && night.price > base;
    });
    return {
        percent: Math.round((multiplier - 1) * 10000) / 100,
        nights: nights.length,
        surcharge: Math.round(nights.reduce((total, night) => total + night.price - base, 0) * 100) / 100
    };
}
export interface StayRateGroup {
    kind: 'weekday' | 'weekend';
    price: number;
    nights: number;
    total: number;
}
/** Groups actual quoted nights by calendar tariff and unit price, without losing rounding. */
export function groupedStayRates(offer: Offer): StayRateGroup[] {
    const groups = new Map<string, StayRateGroup>();
    for (const night of offer.nights) {
        const day = new Date(night.date + 'T00:00:00Z').getUTCDay();
        const kind = day === 0 || day === 6 ? 'weekend' : 'weekday';
        const key = `${kind}:${night.price}`;
        const group = groups.get(key) ?? {
            kind, price: night.price, nights: 0, total: 0
        };
        group.nights++;
        group.total = Math.round((group.total + night.price) * 100) / 100;
        groups.set(key, group);
    }
    return [...groups.values()].sort((a, b) => Number(a.kind === 'weekend') - Number(b.kind === 'weekend') || a.price - b.price);
}
