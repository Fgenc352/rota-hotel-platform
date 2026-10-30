import type { NightPrice } from './night-price.model';
export interface Offer {
    roomTypeId: string;
    name: string;
    capacity: number;
    remaining: number;
    nightlyPrice: number;
    total: number;
    currency: string;
    minNights: number;
    baseNightlyPrice?: number;
    weekendMultiplier?: number;
    nights: NightPrice[];
}
