import { confirmationScrollTop, centeredViewportOffset } from './booking-frame-message';
export function bookingWidgetOffset(viewportHeight: number, headerHeight: number, widgetHeight: number): number {
    return centeredViewportOffset(viewportHeight, headerHeight, widgetHeight);
}
export function scrollToBookingWidget(document: Document, window: Window): void {
    const widget = document.querySelector<HTMLElement>('showcase-booking-embed#booking, #booking showcase-booking-embed');
    const frame = widget?.querySelector<HTMLIFrameElement>('iframe');
    if (!frame)
        return;
    const header = document.querySelector('.signature-header, .site-header, .peri-header, .atelier-header, .collection-header');
    const headerHeight = header?.getBoundingClientRect().height || 0;
    const box = frame.getBoundingClientRect();
    window.scrollTo({
        top: confirmationScrollTop(box.top, window.scrollY, 0, headerHeight, window.innerHeight, box.height),
        behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth'
    });
}
