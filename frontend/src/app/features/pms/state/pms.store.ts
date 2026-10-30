import { Injectable, computed, inject, signal } from '@angular/core';
import { BookingApi } from '@booking/data-access/booking-api.service';
import type { ServiceOption } from '@booking/models/service-option.model';
import type { RoomDetails } from '@booking/models/room-details.model';
import { AuthApi } from '@core/auth/auth-api.service';
import { ReservationApi } from '@crm/data-access/reservation-api.service';
import type { Booking } from '@crm/models/booking.model';
import type { ReservationSearch } from '@crm/models/reservation-search.model';
import { ActionState } from '@shared/state/action-state.service';
import type { Page } from '@shared/models/page.model';
import type { SortSelection } from '@shared/models/sort.model';
import { PmsApi } from '../data-access/pms-api.service';
import type { Housekeeping, RoomAuditPage, PmsBoard, ReservationServices } from '../models/pms.model';
@Injectable()
export class PmsStore {
    readonly actions = inject(ActionState);
    private readonly api = inject(PmsApi);
    private readonly reservations = inject(ReservationApi);
    private readonly catalog = inject(BookingApi);
    readonly auth = inject(AuthApi);
    readonly board = signal<PmsBoard>({ rooms: [], assignments: [] });
    readonly bookings = signal<Page<Booking>>({ items: [], total: 0 });
    readonly history = signal<RoomAuditPage>({
        items: [], total: 0, page: 0, size: 20
    });
    readonly historyPage = signal(0);
    readonly serviceCatalog = signal<ServiceOption[]>([]);
    readonly roomTypes = signal<RoomDetails[]>([]);
    readonly servicePlan = signal<ReservationServices | null>(null);
    readonly selected = signal<Booking | null>(null);
    readonly page = signal(0);
    readonly sort = signal<SortSelection>({ field: 'CHECK_IN', direction: 'ASC' });
    private filters: ReservationSearch = {};
    readonly occupied = computed(() => this.board().rooms.filter(room => room.occupantId).length);
    readonly ready = computed(() => this.board().rooms.filter(room => !room.occupantId && room.housekeeping === 'CLEAN').length);
    readonly dirty = computed(() => this.board().rooms.filter(room => room.housekeeping === 'DIRTY').length);
    async initialize() {
        await this.actions.run(async () => {
            await this.load();
            const key = this.auth.identity()?.hotelKey;
            if (key) {
                const [rooms, services] = await Promise.all([this.catalog.rooms(key), this.catalog.services(key)]);
                this.roomTypes.set(rooms);
                this.serviceCatalog.set(services);
            }
        });
    }
    private async load() {
        const [board, bookings, history] = await Promise.all([this.api.board(), this.reservations.list(this.page(), this.sort(), this.filters), this.api.history(this.historyPage())]);
        this.history.set(history);
        this.board.set(board);
        this.bookings.set(bookings);
        const servicePlan = this.servicePlan();
        if (servicePlan)
            this.servicePlan.set(await this.api.services(servicePlan.reservationId));
    }
    historyGo(page: number) {
        return this.actions.run(async () => {
            const history = await this.api.history(page);
            this.historyPage.set(page);
            this.history.set(history);
        });
    }
    refresh() {
        return this.actions.run(() => this.load());
    }
    filter(filters: ReservationSearch) {
        this.filters = filters;
        this.page.set(0);
        return this.refresh();
    }
    order(sort: SortSelection) {
        this.sort.set(sort);
        this.page.set(0);
        return this.refresh();
    }
    go(page: number) {
        this.page.set(page);
        return this.refresh();
    }
    open(booking: Booking) {
        this.selected.set(booking);
    }
    openId(id: string) {
        return this.actions.run(async () => {
            this.selected.set(await this.reservations.detail(id));
        });
    }
    assign(reservation: string, room: string) {
        return this.actions.run(async () => {
            await this.api.assign(reservation, room);
            await this.load();
            this.actions.notice.set('Oda atandı. İşlem geçmişine kaydedildi.');
        });
    }
    housekeeping(room: string, state: Housekeeping) {
        return this.actions.run(async () => {
            await this.api.housekeeping(room, state);
            await this.load();
            this.actions.notice.set('Odanın temizlik durumu güncellendi.');
        });
    }
    openServices(booking: Booking) {
        const id = booking.id || booking.reservation_id;
        if (!id)
            return;
        this.servicePlan.set(null);
        return this.actions.run(async () => {
            this.servicePlan.set(await this.api.services(id));
        });
    }
    changeStatus(status: string) {
        const booking = this.selected();
        const id = booking?.id || booking?.reservation_id;
        if (!id)
            return;
        if (status === 'CHECKED_IN' && !this.board().assignments.some(row => row.reservationId === id)) {
            this.actions.error.set('PMS üzerinden giriş yapmak için önce bir oda atayın.');
            return;
        }
        if (['CANCELLED', 'NO_SHOW'].includes(status) && !window.confirm('Rezervasyon durumunu değiştirmek istediğinize emin misiniz?'))
            return;
        return this.actions.run(async () => {
            await this.reservations.changeStatus(id, status);
            this.selected.set(await this.reservations.detail(id));
            await this.load();
            this.actions.notice.set('Rezervasyon güncellendi. PMS bildirimi kalıcı kuyruğa alındı.');
        });
    }
}
