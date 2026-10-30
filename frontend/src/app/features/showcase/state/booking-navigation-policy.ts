import type { StayInput } from '@booking/models/stay-input.model';
export type BookingStep = 1 | 2 | 3 | 4;
export interface BookingNavigationState {
    busy: boolean;
    confirmed: boolean;
    quoteCurrent: boolean;
    roomAvailable: boolean;
}
export function stayFingerprint(stay: StayInput): string {
    return JSON.stringify([
        stay.checkIn, stay.checkOut, stay.adults, stay.children
    ]);
}
export function canVisitBookingStep(step: BookingStep, state: BookingNavigationState): boolean {
    if (state.busy || state.confirmed)
        return false;
    if (step === 1)
        return true;
    if (step === 2)
        return state.quoteCurrent;
    if (step === 3)
        return state.quoteCurrent && state.roomAvailable;
    return false;
}
