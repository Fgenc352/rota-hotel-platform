import { Component, ElementRef, HostListener, afterNextRender, computed, inject, input, output, viewChild, OnDestroy } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import type { RoomGalleryRequest } from '@shared/utils/room-gallery-message';
@Component({
    selector: 'showcase-hosted-room-gallery', standalone: true,
    templateUrl: './hosted-room-gallery.component.html', styleUrl: './hosted-room-gallery.component.css'
})
export class HostedRoomGalleryComponent implements OnDestroy {
    readonly hotelKey = input.required<string>();
    readonly request = input.required<RoomGalleryRequest>();
    readonly finished = output<boolean>();
    private readonly sanitizer = inject(DomSanitizer);
    private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
    private readonly frame = viewChild.required<ElementRef<HTMLIFrameElement>>('frame');
    private previousOverflow = '';
    private previousGutter = '';
    private scrollY = 0;
    private opened = false;
    readonly url = computed(() => {
        const request = this.request();
        const query = new URLSearchParams({
            hotel: this.hotelKey(), room: request.roomId,
            currency: request.currency, lang: request.language, selectable: String(request.selectable)
        });
        return this.sanitizer.bypassSecurityTrustResourceUrl('/room-gallery?' + query);
    });
    constructor() {
        afterNextRender(() => {
            this.opened = true;
            this.scrollY = window.scrollY;
            this.previousOverflow = document.documentElement.style.overflow;
            this.previousGutter = document.documentElement.style.scrollbarGutter;
            document.documentElement.style.scrollbarGutter = 'stable';
            document.documentElement.style.overflow = 'hidden';
            this.dialog().nativeElement.showModal();
        });
    }
    @HostListener('window:message', ['$event'])
    message(event: MessageEvent) {
        if (event.origin !== location.origin || event.source !== this.frame().nativeElement.contentWindow)
            return;
        if (event.data?.roomId !== this.request().roomId)
            return;
        if (event.data.type === 'rota:gallery:close')
            this.finished.emit(false);
        if (event.data.type === 'rota:gallery:select' && this.request().selectable)
            this.finished.emit(true);
    }
    ngOnDestroy() {
        if (!this.opened)
            return;
        this.dialog().nativeElement.close();
        document.documentElement.style.overflow = this.previousOverflow;
        document.documentElement.style.scrollbarGutter = this.previousGutter;
        window.scrollTo({ top: this.scrollY, behavior: 'instant' });
    }
}
