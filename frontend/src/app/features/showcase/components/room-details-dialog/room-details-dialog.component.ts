import { catalogText } from '@booking/presentation/catalog-translations';
import { IconComponent } from '@shared/components/icon/icon.component';
import { Component, ElementRef, afterNextRender, input, output, signal, viewChild, OnDestroy, HostListener } from '@angular/core';
import type { RoomDetails } from '@booking/models/room-details.model';
import type { BookingCurrency, BookingLanguage } from '@booking/models/booking-preferences.model';
@Component({
    selector: 'showcase-room-details-dialog',
    standalone: true,
    imports: [IconComponent],
    templateUrl: './room-details-dialog.component.html',
    styleUrl: './room-details-dialog.component.css'
})
export class RoomDetailsDialogComponent implements OnDestroy {
    readonly room = input.required<RoomDetails>();
    readonly currency = input<BookingCurrency>('TRY');
    readonly language = input<BookingLanguage>('tr');
    readonly eurRate = input(1);
    readonly closed = output<void>();
    readonly selected = output<void>();
    readonly selectable = input(true);
    readonly hosted = input(false);
    private hostTimer: ReturnType<typeof setTimeout> | undefined;
    readonly photo = signal(0);
    readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
    readonly text = (value: string) => catalogText(value, this.language());
    readonly t = (tr: string, en: string) => this.language() === 'tr' ? tr : en;
    constructor() {
        afterNextRender(() => {
            if (window.parent !== window && !this.hosted()) {
                window.parent.postMessage({
                    type: 'rota:gallery:open', roomId: this.room().id,
                    currency: this.currency(), language: this.language(), selectable: this.selectable()
                }, '*');
                // Older custom hosts can still use the in-frame gallery.
                this.hostTimer = setTimeout(() => this.dialog().nativeElement.showModal(), 1500);
            }
            else {
                this.dialog().nativeElement.showModal();
            }
        });
    }
    @HostListener('window:message', ['$event'])
    hostMessage(event: MessageEvent) {
        if (this.hosted() || event.source !== window.parent || event.data?.roomId !== this.room().id)
            return;
        if (event.data.type === 'rota:gallery:hosted') {
            clearTimeout(this.hostTimer);
            if (this.dialog().nativeElement.open)
                this.dialog().nativeElement.close();
        }
        if (event.data.type === 'rota:gallery:result' && typeof event.data.selected === 'boolean') {
            clearTimeout(this.hostTimer);
            if (event.data.selected && this.selectable())
                this.selected.emit();
            else
                this.closed.emit();
        }
    }
    outside(event: MouseEvent) {
        if (event.target === this.dialog().nativeElement)
            this.closed.emit();
    }
    movePhoto(event: Event, direction: number) {
        event.preventDefault();
        const length = this.room().images.length;
        if (length)
            this.photo.update(value => (value + direction + length) % length);
    }
    price() {
        return new Intl.NumberFormat(this.language() === 'tr' ? 'tr-TR' : 'en-GB', {
            style: 'currency', currency: this.currency(), minimumFractionDigits: 0, maximumFractionDigits: 2
        }).format(this.currency() === 'EUR' ? this.room().nightlyPrice / this.eurRate() : this.room().nightlyPrice);
    }
    ngOnDestroy() {
        clearTimeout(this.hostTimer);
        this.dialog().nativeElement.close();
    }
}
