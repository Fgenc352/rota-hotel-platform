import { confirmationScrollTop } from '@shared/utils/booking-frame-message';
import { Component, ElementRef, afterNextRender, inject, input, output, viewChild } from '@angular/core';
import { BookingPresentation } from '../../presentation/booking-presentation.service';
import type { Receipt } from '../../models/receipt.model';
@Component({
    selector: 'booking-confirmation',
    standalone: true,
    styleUrl: './booking-confirmation.component.css',
    templateUrl: './booking-confirmation.component.html'
})
export class BookingConfirmationComponent {
    private readonly presentation = inject(BookingPresentation);
    readonly t = this.presentation.t;
    readonly money = this.presentation.money;
    readonly receipt = input.required<Receipt>();
    readonly newBooking = output<void>();
    private readonly card = viewChild.required<ElementRef<HTMLElement>>('confirmation');
    constructor() {
        afterNextRender(() => {
            const card = this.card().nativeElement;
            card.focus({ preventScroll: true });
            const cardHeight = card.getBoundingClientRect().height;
            const top = Math.max(0, card.getBoundingClientRect().top + window.scrollY);
            if (window.parent !== window) {
                // Remove any leftover inner-frame scroll, then ask the host to reveal this card.
                window.scrollTo({ top: 0, behavior: 'instant' });
                const root = card.closest('main');
                const height = Math.ceil(root?.getBoundingClientRect().height || document.body.scrollHeight);
                window.parent.postMessage({
                    type: 'rota:booking:confirmed', height, top, cardHeight
                }, '*');
            }
            else {
                window.scrollTo({ top: confirmationScrollTop(0, 0, top, 0, window.innerHeight, cardHeight),
                    behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth' });
            }
        });
    }
}
