import { AuthSession } from '@core/auth/auth-session.service';
import type { GuestNote } from '../../models/guest-note.model';
import { noteDeletionState } from '../../utils/note-deletion';
import { RouterLink } from '@angular/router';
import { Component, DestroyRef, inject, signal, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActionState } from '@shared/state/action-state.service';
import { IconComponent } from '@shared/components/icon/icon.component';
import { StayDatesComponent } from '@shared/components/stay-dates/stay-dates.component';
import { HotelTimestampPipe } from '@shared/pipes/hotel-timestamp.pipe';
import { stayNights } from '@shared/utils/display-date';
import { money } from '@shared/utils/currency';
import { label } from '../../utils/reservation-status';
import { GuestApi } from '../../data-access/guest-api.service';
import type { GuestDetail } from '../../models/guest-detail.model';
@Component({
    selector: 'crm-guest-detail',
    standalone: true,
    providers: [ActionState],
    imports: [
        RouterLink, CommonModule, FormsModule, IconComponent, StayDatesComponent, HotelTimestampPipe
    ],
    styleUrl: './guest-detail.component.css',
    templateUrl: './guest-detail.component.html'
})
export class GuestDetailComponent {
    readonly actions = inject(ActionState);
    readonly busy = this.actions.busy;
    readonly money = money;
    readonly label = label;
    readonly nights = stayNights;
    private readonly api = inject(GuestApi);
    readonly guestId = input.required<string>();
    readonly closed = output<void>();
    readonly detail = signal<GuestDetail | null>(null);
    readonly pendingNoteDeletion = signal<string | null>(null);
    private readonly session = inject(AuthSession);
    private readonly now = signal(Date.now());
    note = '';
    tag = '';
    constructor() {
        const timer = setInterval(() => this.now.set(Date.now()), 1000);
        inject(DestroyRef).onDestroy(() => clearInterval(timer));
    }
    noteDeletionState(note: GuestNote) {
        return noteDeletionState(note, this.session.username(), this.now());
    }
    noteDeletionHint(note: GuestNote) {
        const state = this.noteDeletionState(note);
        return state === 'not-author' ? 'Bu notu yalnızca yazan personel silebilir.'
            : state === 'expired' ? '24 saatlik silme süresi doldu.' : 'Not eklendikten sonraki ilk 24 saat içinde silinebilir.';
    }
    async deleteNote(note: GuestNote) {
        const id = this.detail()?.guest.id;
        if (!id || this.noteDeletionState(note) !== 'available') {
            this.pendingNoteDeletion.set(null);
            return;
        }
        await this.actions.run(async () => {
            await this.api.deleteNote(id, note.id);
            this.pendingNoteDeletion.set(null);
            this.detail.set(await this.api.detail(id));
        });
    }
    ngOnChanges() {
        this.pendingNoteDeletion.set(null);
        this.detail.set(null);
        void this.actions.run(async () => this.detail.set(await this.api.detail(this.guestId())));
    }
    async addNote() {
        const id = this.detail()?.guest.id;
        if (!id || !this.note.trim()) {
            return;
        }
        await this.actions.run(async () => {
            await this.api.addNote(id, this.note);
            this.note = '';
            this.detail.set(await this.api.detail(id));
        });
    }
    async addTag() {
        const id = this.detail()?.guest.id;
        if (!id || !this.tag.trim()) {
            return;
        }
        await this.actions.run(async () => {
            await this.api.addTag(id, this.tag);
            this.tag = '';
            this.detail.set(await this.api.detail(id));
        });
    }
    async removeTag(tag: string) {
        const id = this.detail()?.guest.id;
        if (!id) {
            return;
        }
        await this.actions.run(async () => {
            await this.api.removeTag(id, tag);
            this.detail.set(await this.api.detail(id));
        });
    }
}
