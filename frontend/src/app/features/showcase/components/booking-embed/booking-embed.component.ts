import { HostedRoomGalleryComponent } from '../hosted-room-gallery/hosted-room-gallery.component';
import { readRoomGalleryRequest, type RoomGalleryRequest } from '@shared/utils/room-gallery-message';
import { readBookingFrameMessage, confirmationScrollTop } from '@shared/utils/booking-frame-message';
import { Injector, DestroyRef, afterNextRender, Component, computed, inject, input, signal, viewChild, ElementRef, HostListener } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
@Component({
    selector: 'showcase-booking-embed',
    standalone: true,
    imports: [HostedRoomGalleryComponent],
    templateUrl: './booking-embed.component.html',
    styleUrl: './booking-embed.component.css'
})
export class BookingEmbedComponent {
    private readonly destroy = inject(DestroyRef);
    private revealFrame = 0;
    private readonly injector = inject(Injector);
    private readonly sanitizer = inject(DomSanitizer);
    readonly hotelKey = input.required<string>();
    readonly preferredRoom = input<string | null>(null);
    readonly variant = input<'premium' | 'signature' | 'atelier' | 'maison' | 'quiet'>('premium');
    readonly gallery = signal<RoomGalleryRequest | null>(null);
    readonly height = signal(660);
    private readonly frame = viewChild<ElementRef<HTMLIFrameElement>>('frame');
    readonly url = computed(() => {
        const key = this.hotelKey();
        if (key !== 'lagos-beach' && key !== 'peri-stone')
            return null;
        const query = new URLSearchParams({ hotel: key });
        const preferred = this.preferredRoom();
        if (preferred)
            query.set('room', preferred);
        return this.sanitizer.bypassSecurityTrustResourceUrl('/widget-' + this.variant() + '?' + query);
    });
    constructor() {
        this.destroy.onDestroy(() => cancelAnimationFrame(this.revealFrame));
    }
    finishGallery(selected: boolean) {
        const request = this.gallery();
        if (!request)
            return;
        this.frame()?.nativeElement.contentWindow?.postMessage({
            type: 'rota:gallery:result', roomId: request.roomId, selected
        }, location.origin);
        this.gallery.set(null);
    }
    @HostListener('window:message', ['$event'])
    resize(event: MessageEvent) {
        if (event.origin !== location.origin || event.source !== this.frame()?.nativeElement.contentWindow)
            return;
        const gallery = readRoomGalleryRequest(event.data);
        if (gallery) {
            this.gallery.set(gallery);
            this.frame()?.nativeElement.contentWindow?.postMessage({ type: 'rota:gallery:hosted', roomId: gallery.roomId }, location.origin);
            return;
        }
        const data = readBookingFrameMessage(event.data);
        if (!data)
            return;
        this.height.set(Math.ceil(data.height) + 4);
        if (data.type === 'rota:booking:confirmed') {
            // Wait for Angular to apply the smaller frame before measuring its page position.
            afterNextRender(() => {
                cancelAnimationFrame(this.revealFrame);
                this.revealFrame = requestAnimationFrame(() => {
                    this.revealFrame = requestAnimationFrame(() => {
                        if (this.destroy.destroyed)
                            return;
                        const frame = this.frame()?.nativeElement;
                        if (!frame)
                            return;
                        const header = document.querySelector('.signature-header, .site-header, .peri-header, .atelier-header, .collection-header');
                        const headerHeight = header?.getBoundingClientRect().height || 0;
                        window.scrollTo({
                            top: confirmationScrollTop(frame.getBoundingClientRect().top, window.scrollY, data.top, headerHeight, window.innerHeight, data.cardHeight),
                            behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth'
                        });
                    });
                });
            }, { injector: this.injector });
        }
    }
}
