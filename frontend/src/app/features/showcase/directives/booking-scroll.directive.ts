import { DOCUMENT, Location } from '@angular/common';
import { DestroyRef, Directive, HostBinding, HostListener, inject } from '@angular/core';
import { scrollToBookingWidget } from '@shared/utils/booking-widget-scroll';
@Directive({ selector: 'a[bookingScroll]', standalone: true })
export class BookingScrollDirective {
    private readonly document = inject(DOCUMENT);
    private readonly location = inject(Location);
    private frame = 0;
    constructor() {
        inject(DestroyRef).onDestroy(() => cancelAnimationFrame(this.frame));
    }
    @HostBinding('attr.href')
    get href(): string {
        const url = this.document.defaultView?.location;
        return url ? url.pathname + url.search + '#booking' : '#booking';
    }
    @HostListener('click', ['$event'])
    click(event: MouseEvent) {
        if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey)
            return;
        const window = this.document.defaultView;
        if (!window)
            return;
        event.preventDefault();
        this.location.replaceState(this.href, '', this.location.getState());
        cancelAnimationFrame(this.frame);
        this.frame = requestAnimationFrame(() => {
            this.frame = requestAnimationFrame(() => scrollToBookingWidget(this.document, window));
        });
    }
}
