import { Directive, DestroyRef, ElementRef, afterNextRender, inject } from '@angular/core';
@Directive({ selector: '[bookingWidgetResize]', standalone: true })
export class WidgetResizeDirective {
    private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
    private readonly destroy = inject(DestroyRef);
    constructor() {
        afterNextRender(() => {
            let frame = 0;
            const observer = new ResizeObserver(() => {
                cancelAnimationFrame(frame);
                frame = requestAnimationFrame(() => {
                    if (window.parent !== window) {
                        // Only dimensions are shared; the parent validates source, origin and bounds.
                        window.parent.postMessage({ type: 'rota:premium-widget:resize',
                            height: Math.ceil(this.element.nativeElement.getBoundingClientRect().height) }, '*');
                    }
                });
            });
            observer.observe(this.element.nativeElement);
            this.destroy.onDestroy(() => {
                observer.disconnect();
                cancelAnimationFrame(frame);
            });
        });
    }
}
