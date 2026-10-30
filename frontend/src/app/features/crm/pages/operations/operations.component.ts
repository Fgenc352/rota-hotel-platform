import { environment } from '@environment';
import { Component, computed, inject, signal } from '@angular/core';
import { IconComponent } from '@shared/components/icon/icon.component';
import { ActionState } from '@shared/state/action-state.service';
import { OperationsApi } from '../../data-access/operations-api.service';
import type { Analytics } from '../../models/analytics.model';
import type { FailedEvent } from '../../models/failed-event.model';
@Component({
    selector: 'crm-operations',
    standalone: true,
    providers: [ActionState],
    imports: [IconComponent],
    styleUrl: './operations.component.css',
    templateUrl: './operations.component.html'
})
export class OperationsComponent {
    readonly pmsDashboardUrl = environment.pmsDashboardUrl;
    readonly actions = inject(ActionState);
    readonly error = this.actions.error;
    readonly busy = this.actions.busy;
    readonly notice = this.actions.notice;
    private readonly api = inject(OperationsApi);
    readonly analytics = signal<Analytics | null>(null);
    readonly failed = signal<FailedEvent[]>([]);
    readonly pending = computed(() => this.analytics()?.deliveries?.reduce((sum, item) => sum + item.pending, 0) ?? 0);
    readonly attention = computed(() => this.analytics()?.deliveries?.reduce((sum, item) => sum + item.dead, 0) ?? 0);
    readonly stageLabels: Record<string, string> = {
        SEARCH: 'Oda araması', ROOM_SELECTED: 'Oda seçimi', FORM: 'Misafir bilgileri', CONFIRMED: 'Rezervasyon onayı'
    };
    width(stage: string) {
        const maximum = Math.max(1, ...[
            'SEARCH', 'ROOM_SELECTED', 'FORM', 'CONFIRMED'
        ].map(key => this.count(key)));
        return this.count(stage) / maximum * 100;
    }
    consumerLabel(consumer: string) {
        return consumer.toUpperCase() === 'CRM' ? 'Misafir profilleri' : consumer.toUpperCase() === 'PMS' ? 'Otel yönetim sistemi' : consumer.toUpperCase() === 'KAFKA' ? 'Kafka olay aktarımı' : consumer;
    }
    ngOnInit() {
        void this.refresh();
    }
    refresh() {
        return this.actions.run(() => this.loadOperations());
    }
    async loadOperations() {
        const [analytics, failed] = await Promise.all([this.api.analytics(), this.api.failed()]);
        this.analytics.set(analytics);
        this.failed.set(failed);
    }
    count(stage: string) {
        return this.analytics()?.funnel?.find(f => f.stage === stage)?.sessions || 0;
    }
    conversion() {
        const search = this.count('SEARCH');
        return search ? Math.round(this.count('CONFIRMED') / search * 100) : 0;
    }
    async retry(id: string) {
        await this.actions.run(async () => {
            await this.api.retry(id);
            await this.loadOperations();
            this.notice.set('Bildirim yeniden işleme alındı.');
        });
    }
}
