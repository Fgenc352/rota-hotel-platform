import { Component, ElementRef, OnDestroy, afterNextRender, input, output, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HotelTimestampPipe } from '@shared/pipes/hotel-timestamp.pipe';
import { dailyStatusLabel, workflow, type DailyService, type DailyTask, type DailyAudit } from '../../models/daily-operations.model';
@Component({
    selector: 'pms-daily-service-editor', standalone: true, imports: [FormsModule, HotelTimestampPipe],
    templateUrl: './daily-service-editor.component.html', styleUrl: './daily-service-editor.component.css'
})
export class DailyServiceEditorComponent implements OnDestroy {
    readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
    constructor() {
        afterNextRender(() => this.dialog().nativeElement.showModal());
    }
    ngOnDestroy() {
        this.dialog().nativeElement.close();
    }
    readonly item = input.required<DailyService>();
    readonly history = input<DailyAudit[]>([]);
    readonly busy = input(false);
    readonly error = input('');
    readonly saved = output<DailyTask>();
    readonly closed = output<void>();
    readonly label = dailyStatusLabel;
    draft!: DailyTask;
    ngOnInit() {
        this.draft = { ...this.item().task };
    }
    finished() {
        return ['SERVED', 'COMPLETED', 'DELIVERED'].includes(this.item().task.status);
    }
    states() {
        const states = workflow(this.item().task.code);
        const current = states.indexOf(this.item().task.status);
        return states.filter((_, i) => i === current || i === current + 1 || (states.includes('SERVED') && i > current));
    }
    summarize(json: string) {
        try {
            const task = JSON.parse(json);
            return [
                task.status ? dailyStatusLabel(task.status) : 'Henüz işlem yok', task.date,
                task.time?.slice(0, 5), task.pickup, task.destination, task.plate,
                task.passengers ? task.passengers + ' yolcu' : '', task.notes
            ].filter(Boolean).join(' · ');
        }
        catch {
            return '—';
        }
    }
}
