import { Injectable, inject, signal, computed } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { stayNights } from '@shared/utils/display-date';
import type { ServiceOption, ServiceLine } from '../models/service-option.model';
import { FormBuilder, Validators } from '@angular/forms';
import { BookingApi } from '../data-access/booking-api.service';
import { BookingPresentation } from '../presentation/booking-presentation.service';
import { bookingErrorMessage } from '../presentation/booking-error-message';
import { BookingAttempt } from './booking-attempt';
import type { Offer } from '../models/offer.model';
import type { Receipt } from '../models/receipt.model';
@Injectable()
export class BookingStore {
    private readonly api = inject(BookingApi);
    private readonly fb = inject(FormBuilder);
    readonly key = new URLSearchParams(location.search).get('hotel') || 'lagos-beach';
    readonly sessionId = crypto.randomUUID();
    private readonly presentation = inject(BookingPresentation);
    readonly lang = this.presentation.lang;
    readonly currency = this.presentation.currency;
    readonly hotel = this.presentation.hotel;
    readonly t = this.presentation.t;
    readonly text = this.presentation.text;
    readonly money = this.presentation.money;
    readonly offers = signal<Offer[]>([]);
    readonly selected = signal<Offer | null>(null);
    readonly receipt = signal<Receipt | null>(null);
    readonly busy = signal(false);
    readonly searched = signal(false);
    readonly error = signal('');
    private readonly attempt = new BookingAttempt();
    readonly searchForm = this.fb.nonNullable.group({
        checkIn: ['', [Validators.required]],
        checkOut: ['', [Validators.required]],
        adults: [2, [Validators.required, Validators.min(1), Validators.max(10)]],
        children: [0, [Validators.required, Validators.min(0), Validators.max(10)]]
    });
    readonly guestForm = this.fb.nonNullable.group({
        firstName: ['', [Validators.required, Validators.maxLength(100)]],
        lastName: ['', [Validators.required, Validators.maxLength(100)]],
        email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
        phone: ['', [Validators.required, Validators.pattern(/^\+[1-9][0-9]{7,14}$/)]],
        privacyAccepted: [false, Validators.requiredTrue],
        marketingAllowed: [false]
    });
    readonly services = signal<ServiceOption[]>([]);
    readonly servicesReady = signal(false);
    readonly selectedServices = signal<string[]>([]);
    private readonly stayValue = toSignal(this.searchForm.valueChanges, { initialValue: this.searchForm.getRawValue() });
    readonly serviceLines = computed<ServiceLine[]>(() => {
        const stay = this.stayValue();
        const nights = stayNights(stay.checkIn || '', stay.checkOut || '');
        const people = Number(stay.adults || 0) + Number(stay.children || 0);
        return this.services().filter(service => service.included || this.selectedServices().includes(service.code)).map(service => {
            const quantity = service.unit === 'PER_STAY' ? 1 : service.unit === 'PER_NIGHT' ? nights : nights * people;
            const unitPrice = service.included ? 0 : Number(service.price);
            return {
                code: service.code, name: service.name, unit: service.unit, unitPrice, quantity,
                total: unitPrice * quantity, included: service.included
            };
        });
    });
    readonly extrasTotal = computed(() => this.serviceLines().reduce((sum, line) => sum + line.total, 0));
    readonly bookingTotal = computed(() => Number(this.selected()?.total || 0) + this.extrasTotal());
    toggleService(code: string, checked: boolean) {
        if (this.busy())
            return;
        this.selectedServices.update(codes => checked ? [...new Set([...codes, code])] : codes.filter(value => value !== code));
    }
    async loadServices() {
        this.servicesReady.set(false);
        try {
            this.services.set(await this.api.services(this.key));
            this.servicesReady.set(true);
        }
        catch (error) {
            this.error.set(this.message(error));
        }
    }
    async initialize() {
        try {
            const hotel = await this.api.hotel(this.key);
            this.hotel.set(hotel);
            await this.loadServices();
            const today = new Intl.DateTimeFormat('en-CA', {
                timeZone: hotel.timezone,
                year: 'numeric',
                month: '2-digit',
                day: '2-digit'
            }).format(new Date());
            const next = new Date(today + 'T12:00:00Z');
            next.setUTCDate(next.getUTCDate() + 1);
            this.searchForm.patchValue({
                checkIn: today,
                checkOut: next.toISOString().slice(0, 10)
            });
        }
        catch (error) {
            this.error.set(this.message(error));
        }
    }
    async search() {
        this.searchForm.markAllAsTouched();
        if (this.searchForm.invalid || this.busy()) {
            return;
        }
        this.busy.set(true);
        this.error.set('');
        this.selected.set(null);
        this.receipt.set(null);
        this.offers.set([]);
        try {
            const v = this.searchForm.getRawValue();
            if (v.checkOut <= v.checkIn) {
                throw new Error(this.t('Çıkış tarihi girişten sonra olmalıdır.', 'Check-out must be after check-in.'));
            }
            this.offers.set(await this.api.availability(this.key, v));
            this.searched.set(true);
            this.track('SEARCH');
        }
        catch (e) {
            this.error.set(this.message(e));
        }
        finally {
            this.busy.set(false);
        }
    }
    select(offer: Offer) {
        this.selected.set(offer);
        this.error.set('');
        this.track('ROOM_SELECTED');
        this.track('FORM');
    }
    async book() {
        if (!this.servicesReady()) {
            this.error.set(this.t('Hizmet bilgileri yüklenemedi. Lütfen tekrar yükleyin.', 'Services are unavailable. Please reload them.'));
            return;
        }
        this.guestForm.markAllAsTouched();
        if (this.guestForm.invalid || !this.selected() || this.busy()) {
            return;
        }
        this.busy.set(true);
        this.error.set('');
        try {
            const body = {
                ...this.searchForm.getRawValue(),
                ...this.guestForm.getRawValue(),
                roomTypeId: this.selected()!.roomTypeId,
                sessionId: this.sessionId,
                serviceCodes: [...this.selectedServices()].sort()
            };
            const key = this.attempt.keyFor(body);
            this.receipt.set(await this.api.book(this.key, body, key));
            this.attempt.complete();
            this.selected.set(null);
        }
        catch (e) {
            this.error.set(this.message(e));
        }
        finally {
            this.busy.set(false);
        }
    }
    startNewBooking() {
        this.selectedServices.set([]);
        this.attempt.complete();
        this.receipt.set(null);
        this.selected.set(null);
        this.searched.set(false);
    }
    private message(error: unknown) {
        return bookingErrorMessage(error, this.lang());
    }
    private track(stage: string) {
        void this.api.track(this.key, this.sessionId, stage).catch(() => {
        });
    }
}
