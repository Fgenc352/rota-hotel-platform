import type { GuestNote } from './guest-note.model';
import type { Guest } from './guest.model';
import type { Booking } from './booking.model';
export interface GuestDetail {
    guest: Guest;
    reservations: Booking[];
    notes: GuestNote[];
    tags: string[];
    emails?: string[];
    phones?: string[];
}
