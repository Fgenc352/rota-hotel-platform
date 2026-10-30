import { Injectable, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BookingStore } from '@booking/state/booking.store';
import { canVisitBookingStep, stayFingerprint } from './booking-navigation-policy';
import type { BookingStep } from './booking-navigation-policy';
@Injectable()
export class BookingNavigation {
    readonly booking = inject(BookingStore);
    private readonly quotedStay = signal('');
    private readonly lastRoomId = signal<string | null>(null);
    private readonly formRevision = signal(0);
    private readonly currentStay = computed(() => {
        this.formRevision();
        return stayFingerprint(this.booking.searchForm.getRawValue());
    });
    readonly quoteCurrent = computed(() => !!this.quotedStay() && this.quotedStay() === this.currentStay());
    readonly rememberedRoom = computed(() => this.booking.offers().find(room => room.roomTypeId === this.lastRoomId()));
    readonly step = computed<BookingStep>(() => this.booking.receipt() ? 4
        : this.booking.selected() ? 3 : this.booking.searched() ? 2 : 1);
    constructor() {
        this.booking.searchForm.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
            this.formRevision.update(value => value + 1);
            if (this.quotedStay() && !this.quoteCurrent()) {
                this.quotedStay.set('');
                this.lastRoomId.set(null);
                this.booking.offers.set([]);
                this.booking.selected.set(null);
                this.booking.searched.set(false);
            }
        });
    }
    canVisit(step: BookingStep) {
        return canVisitBookingStep(step, {
            busy: this.booking.busy(), confirmed: !!this.booking.receipt(),
            quoteCurrent: this.quoteCurrent(), roomAvailable: !!this.rememberedRoom()
        });
    }
    goTo(step: BookingStep) {
        if (!this.canVisit(step))
            return;
        this.booking.error.set('');
        if (step === 1) {
            this.booking.selected.set(null);
            this.booking.searched.set(false);
        }
        else if (step === 2) {
            this.booking.selected.set(null);
            this.booking.searched.set(true);
        }
        else if (step === 3) {
            this.booking.selected.set(this.rememberedRoom()!);
        }
    }
    async search(): Promise<boolean> {
        if (this.booking.busy() || this.booking.receipt())
            return false;
        const stay = this.currentStay();
        this.lastRoomId.set(null);
        this.quotedStay.set('');
        await this.booking.search();
        if (stay !== this.currentStay()) {
            this.booking.offers.set([]);
            this.booking.searched.set(false);
            return false;
        }
        if (this.booking.searchForm.invalid || this.booking.error() || !this.booking.searched())
            return false;
        this.quotedStay.set(stay);
        return true;
    }
    select(id: string): boolean {
        if (!this.quoteCurrent() || this.booking.busy() || this.booking.receipt())
            return false;
        const offer = this.booking.offers().find(room => room.roomTypeId === id);
        if (!offer)
            return false;
        this.lastRoomId.set(id);
        this.booking.select(offer);
        return true;
    }
    reset() {
        this.quotedStay.set('');
        this.lastRoomId.set(null);
        this.booking.offers.set([]);
        this.booking.startNewBooking();
    }
}
