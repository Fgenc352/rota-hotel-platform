import { Component, inject, signal, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActionState } from '@shared/state/action-state.service';
import { SegmentApi } from '../../data-access/segment-api.service';
@Component({
    selector: 'crm-segment-editor',
    standalone: true,
    imports: [FormsModule],
    styleUrl: './segment-editor.component.css',
    templateUrl: './segment-editor.component.html'
})
export class SegmentEditorComponent {
    readonly actions = inject(ActionState);
    readonly busy = this.actions.busy;
    private readonly api = inject(SegmentApi);
    readonly selected = input.required<string>();
    readonly changed = output<string>();
    readonly segmentEditor = signal(false);
    segmentName = '';
    minStays = 2;
    months = 12;
    marketingRequired = false;
    async createSegment() {
        if (!this.segmentName.trim()) {
            return;
        }
        await this.actions.run(async () => {
            const saved = await this.api.create({
                name: this.segmentName,
                minStays: this.minStays,
                months: this.months,
                marketingRequired: this.marketingRequired
            });
            this.segmentEditor.set(false);
            this.segmentName = '';
            this.changed.emit(saved.id);
        });
    }
    async deleteSegment() {
        if (['ALL', 'RETURNING', 'REENGAGE'].includes(this.selected()) || !window.confirm('Seçili segment silinsin mi?')) {
            return;
        }
        await this.actions.run(async () => {
            await this.api.delete(this.selected());
            this.changed.emit('ALL');
        });
    }
}
