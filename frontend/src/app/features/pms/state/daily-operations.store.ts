import { Injectable, inject, signal } from '@angular/core';
import { ActionState } from '@shared/state/action-state.service';
import { PmsApi } from '../data-access/pms-api.service';
import { type DailyOperations, type DailyService, type DailyTask, type DailyAudit } from '../models/daily-operations.model';
@Injectable()
export class DailyOperationsStore {
    readonly actions = inject(ActionState);
    private readonly api = inject(PmsApi);
    readonly date = signal('');
    readonly data = signal<DailyOperations | null>(null);
    readonly selected = signal<DailyService | null>(null);
    readonly history = signal<DailyAudit[]>([]);
    refresh() {
        return this.actions.run(async () => {
            const data = await this.api.daily(this.date());
            this.date.set(data.date);
            this.data.set(data);
        });
    }
    day(date: string) {
        if (this.actions.busy())
            return;
        this.date.set(date);
        this.selected.set(null);
        this.data.set(null);
        return this.refresh();
    }
    edit(item: DailyService) {
        this.history.set([]);
        this.selected.set(item);
        return this.actions.run(async () => {
            this.history.set(await this.api.dailyHistory(item.task));
        });
    }
    save(task: DailyTask) {
        return this.actions.run(async () => {
            await this.api.dailyUpdate(task);
            this.selected.set(null);
            const data = await this.api.daily(this.date());
            this.date.set(data.date);
            this.data.set(data);
            this.actions.notice.set('Günlük hizmet kaydedildi. Personel ve önceki/sonraki değerler işlem geçmişine yazıldı.');
        });
    }
}
