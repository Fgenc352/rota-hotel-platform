import { ActivatedRoute, Router } from '@angular/router';
import { GuestSearchComponent } from '../../components/guest-search/guest-search.component';
import { emptyGuestSearch } from '../../models/guest-search.model';
import type { GuestSearch } from '../../models/guest-search.model';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActionState } from '@shared/state/action-state.service';
import { IconComponent } from '@shared/components/icon/icon.component';
import { SortControlsComponent } from '@shared/components/sort-controls/sort-controls.component';
import type { SortOption, SortSelection } from '@shared/models/sort.model';
import { GuestApi } from '../../data-access/guest-api.service';
import { SegmentApi } from '../../data-access/segment-api.service';
import type { Guest } from '../../models/guest.model';
import type { Page } from '@shared/models/page.model';
import type { Segment } from '../../models/segment.model';
import { GuestDetailComponent } from '../../components/guest-detail/guest-detail.component';
import { SegmentEditorComponent } from '../../components/segment-editor/segment-editor.component';
@Component({
    selector: 'crm-guest-list',
    standalone: true,
    providers: [ActionState],
    imports: [
        FormsModule, GuestDetailComponent, SegmentEditorComponent, GuestSearchComponent, IconComponent, SortControlsComponent
    ],
    styleUrl: './guest-list.component.css',
    templateUrl: './guest-list.component.html'
})
export class GuestListComponent {
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    readonly actions = inject(ActionState);
    readonly error = this.actions.error;
    readonly busy = this.actions.busy;
    readonly notice = this.actions.notice;
    sort: SortSelection = { field: 'LAST_NAME', direction: 'ASC' };
    readonly sortOptions: SortOption[] = [
        { value: 'FIRST_NAME', label: 'Ad' }, { value: 'LAST_NAME', label: 'Soyad' },
        { value: 'EMAIL', label: 'E-posta' }, { value: 'PHONE', label: 'Telefon' }
    ];
    private readonly api = inject(GuestApi);
    private readonly segmentApi = inject(SegmentApi);
    readonly guests = signal<Page<Guest>>({
        items: [],
        total: 0
    });
    readonly segments = signal<Segment[]>([]);
    readonly detailId = signal<string | null>(null);
    filters = emptyGuestSearch();
    segment = 'ALL';
    page = 0;
    private queryVersion = 0;
    ngOnInit() {
        this.detailId.set(this.route.snapshot.queryParamMap.get('guestId'));
        void this.actions.run(async () => {
            await this.loadGuests();
            this.segments.set(await this.segmentApi.list());
        });
    }
    closeGuest() {
        this.detailId.set(null);
        void this.router.navigate([], {
            relativeTo: this.route, queryParams: { guestId: null }, queryParamsHandling: 'merge'
        });
    }
    openGuest(id: string) {
        this.detailId.set(id);
    }
    async segmentChanged(id: string) {
        this.segment = id;
        this.page = 0;
        try {
            this.segments.set(await this.segmentApi.list());
            await this.loadGuests();
        }
        catch (error) {
            this.actions.error.set(error instanceof Error ? error.message : 'İşlem tamamlanamadı.');
        }
    }
    async loadGuests() {
        const version = ++this.queryVersion;
        const result = await this.api.search('', this.page, this.segment, this.filters, this.sort);
        if (version === this.queryVersion) {
            this.guests.set(result);
        }
    }
    async filterChanged(filters: GuestSearch) {
        this.filters = filters;
        await this.search();
    }
    async sortChanged(sort: SortSelection) {
        this.sort = sort;
        await this.search();
    }
    async search() {
        this.page = 0;
        await this.actions.run(() => this.loadGuests());
    }
    async changePage(delta: number) {
        this.page += delta;
        await this.actions.run(() => this.loadGuests());
    }
}
