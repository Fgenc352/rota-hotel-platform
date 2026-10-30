import { environment } from '@environment';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import type { Booking } from '@crm/models/booking.model';
import { IconComponent } from '@shared/components/icon/icon.component';
import { ActionState } from '@shared/state/action-state.service';
import { SortControlsComponent } from '@shared/components/sort-controls/sort-controls.component';
import { ReservationFiltersComponent } from '@crm/components/reservation-filters/reservation-filters.component';
import { ReservationDetailComponent } from '@crm/components/reservation-detail/reservation-detail.component';
import { RoomBoardComponent } from '../../components/room-board/room-board.component';
import { StayListComponent } from '../../components/stay-list/stay-list.component';
import { AuditTimelineComponent } from '../../components/audit-timeline/audit-timeline.component';
import { ServiceDeskComponent } from '../../components/service-desk/service-desk.component';
import { DailyOperationsComponent } from '../../components/daily-operations/daily-operations.component';
import { ServiceCatalogComponent } from '../../components/service-catalog/service-catalog.component';
import { PmsStore } from '../../state/pms.store';
@Component({
    standalone: true,
    providers: [ActionState, PmsStore],
    imports: [
        DailyOperationsComponent, ServiceCatalogComponent, ServiceDeskComponent, IconComponent, RoomBoardComponent, StayListComponent, AuditTimelineComponent, ReservationFiltersComponent, ReservationDetailComponent, SortControlsComponent
    ],
    templateUrl: './pms-page.component.html', styleUrl: './pms-page.component.css'
})
export class PmsPageComponent {
    readonly pmsDashboardUrl = environment.pmsDashboardUrl;
    readonly view = signal<'stays' | 'rooms' | 'history' | 'services' | 'daily'>('daily');
    readonly pms = inject(PmsStore);
    readonly sortOptions = [
        { value: 'CHECK_IN', label: 'Giriş tarihi' }, { value: 'CHECK_OUT', label: 'Çıkış tarihi' },
        { value: 'GUEST_NAME', label: 'Misafir' }, { value: 'STATUS', label: 'Durum' }, { value: 'CREATED_AT', label: 'Kayıt tarihi' }
    ];
    constructor() {
        const timer = setInterval(() => {
            if (!document.hidden && !this.pms.selected() && !this.pms.actions.busy())
                void this.pms.refresh();
        }, 15000);
        inject(DestroyRef).onDestroy(() => clearInterval(timer));
    }
    async services(booking: Booking) {
        await this.pms.openServices(booking);
        if (this.pms.servicePlan())
            requestAnimationFrame(() => requestAnimationFrame(() => {
                document.querySelector('pms-service-desk')?.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth', block: 'start' });
            }));
    }
    historyPages() {
        return Math.max(1, Math.ceil(this.pms.history().total / this.pms.history().size));
    }
    ngOnInit() {
        void this.pms.initialize();
    }
}
