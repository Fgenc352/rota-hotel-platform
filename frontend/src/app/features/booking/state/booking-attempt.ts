import type { BookingInput } from '../models/booking-input.model';
export class BookingAttempt {
    private readonly keys = new Map<string, string>();
    private readonly createKey: () => string;
    constructor(createKey: () => string = () => crypto.randomUUID()) {
        this.createKey = createKey;
    }
    keyFor(booking: BookingInput): string {
        const fingerprint = JSON.stringify(booking);
        const previous = this.keys.get(fingerprint);
        if (previous) {
            return previous;
        }
        const key = this.createKey();
        this.keys.set(fingerprint, key);
        return key;
    }
    complete(): void {
        this.keys.clear();
    }
}
