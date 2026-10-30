import { ActivatedRoute, Router } from '@angular/router';
import { AuthApi } from '@core/auth/auth-api.service';
import { BookingApi } from '@booking/data-access/booking-api.service';
import type { RoomDetails } from '@booking/models/room-details.model';
import { ReservationFiltersComponent } from '../../components/reservation-filters/reservation-filters.component';
import type { ReservationSearch } from '../../models/reservation-search.model';
import { Component, inject, signal } from '@angular/core';
import { IconComponent } from '@shared/components/icon/icon.component';
import { StayDatesComponent } from '@shared/components/stay-dates/stay-dates.component';
import { SortControlsComponent } from '@shared/components/sort-controls/sort-controls.component';
import type { SortSelection, SortOption } from '@shared/models/sort.model';
import { ReservationDetailComponent } from '../../components/reservation-detail/reservation-detail.component';
import { ActionState } from '@shared/state/action-state.service';
import { money } from '@shared/utils/currency';
import { label, allowed } from '../../utils/reservation-status';
import { ReservationApi } from '../../data-access/reservation-api.service';
import type { Booking } from '../../models/booking.model';
import type { Page } from '@shared/models/page.model';
@Component({
    selector: 'crm-reservations',
    standalone: true,
    providers: [ActionState],
    imports: [
        IconComponent, StayDatesComponent, SortControlsComponent, ReservationDetailComponent, ReservationFiltersComponent
    ],
    styleUrl: './reservations.component.css',
    templateUrl: './reservations.component.html'
})
export class ReservationsComponent {
    readonly actions = inject(ActionState);
    readonly error = this.actions.error;
    readonly busy = this.actions.busy;
    readonly notice = this.actions.notice;
    readonly money = money;
    readonly label = label;
    readonly allowed = allowed;
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly auth = inject(AuthApi);
    private readonly bookingApi = inject(BookingApi);
    readonly rooms = signal<RoomDetails[]>([]);
    filters: ReservationSearch = {};
    private readonly api = inject(ReservationApi);
    readonly reservations = signal<Page<Booking>>({
        items: [],
        total: 0
    });
    readonly selected = signal<Booking | null>(null);
    sort: SortSelection = { field: 'CREATED_AT', direction: 'DESC' };
    readonly sortOptions: SortOption[] = [
        { value: 'CREATED_AT', label: 'Kayıt tarihi' }, { value: 'CHECK_IN', label: 'Giriş tarihi' },
        { value: 'CHECK_OUT', label: 'Çıkış tarihi' }, { value: 'GUEST_NAME', label: 'Misafir adı' },
        { value: 'ROOM_NAME', label: 'Oda tipi' }, { value: 'STATUS', label: 'Durum' }, { value: 'TOTAL', label: 'Toplam tutar' }
    ];
    reservationPage = 0;
    private queryVersion = 0;
    ngOnInit() {
        void this.initialize();
        void this.loadRooms();
    }
    private async initialize() {
        await this.refresh();
        const id = this.route.snapshot.queryParamMap.get('reservationId');
        if (id)
            await this.openId(id);
    }
    async loadRooms() {
        const key = this.auth.identity()?.hotelKey;
        if (!key)
            return;
        try {
            this.rooms.set(await this.bookingApi.rooms(key));
        }
        catch {
            this.error.set('Oda tipi seçenekleri yüklenemedi. Sayfayı yenileyin.');
        }
    }
    async filterChanged(filters: ReservationSearch) {
        this.filters = filters;
        this.reservationPage = 0;
        await this.refresh();
    }
    refresh() {
        return this.actions.run(() => this.loadReservations());
    }
    async loadReservations() {
        const version = ++this.queryVersion;
        const result = await this.api.list(this.reservationPage, this.sort, this.filters);
        if (version === this.queryVersion)
            this.reservations.set(result);
    }
    open(booking: Booking) {
        return this.openId(booking.id || booking.reservation_id!);
    }
    async openId(id: string) {
        await this.actions.run(async () => this.selected.set(await this.api.detail(id)));
    }
    closeDetail() {
        this.selected.set(null);
        void this.router.navigate([], {
            relativeTo: this.route, queryParams: { reservationId: null }, queryParamsHandling: 'merge'
        });
    }
    async sortChanged(sort: SortSelection) {
        this.sort = sort;
        this.reservationPage = 0;
        await this.refresh();
    }
    async changeReservationPage(delta: number) {
        this.reservationPage += delta;
        await this.actions.run(() => this.loadReservations());
    }
    async changeStatus(booking: Booking, status: string) {
        if (status === 'CANCELLED' && !window.confirm('Bu rezervasyonu iptal etmek istiyor musunuz?')) {
            return;
        }
        await this.actions.run(async () => {
            await this.api.changeStatus(booking.id || booking.reservation_id!, status);
            this.closeDetail();
            this.notice.set('Rezervasyon durumu güncellendi.');
            await this.loadReservations();
        });
    }
}
