import { Directive, DestroyRef, ElementRef, NgZone, afterNextRender, inject } from '@angular/core';
@Directive({ selector: '[signatureMotion]', standalone: true })
export class SignatureMotionDirective {
    private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
    private readonly zone = inject(NgZone);
    private readonly destroy = inject(DestroyRef);
    constructor() {
        afterNextRender(() => this.zone.runOutsideAngular(() => {
            const root = this.host.nativeElement;
            const motion = window.matchMedia('(prefers-reduced-motion: reduce)');
            let frame = 0;
            const update = () => {
                const distance = Math.max(1, document.documentElement.scrollHeight - innerHeight);
                root.style.setProperty('--page-progress', String(Math.min(scrollY / distance, 1)));
                root.style.setProperty('--hero-shift', motion.matches ? '0px' : Math.min(scrollY * .12, 100) + 'px');
                root.classList.toggle('has-scrolled', scrollY > 70);
            };
            const onScroll = () => {
                cancelAnimationFrame(frame);
                frame = requestAnimationFrame(update);
            };
            let observer: IntersectionObserver | undefined;
            if (!motion.matches && typeof IntersectionObserver !== 'undefined') {
                observer = new IntersectionObserver(entries => {
                    for (const entry of entries) {
                        if (entry.isIntersecting) {
                            entry.target.classList.add('is-visible');
                            observer?.unobserve(entry.target);
                        }
                    }
                }, { threshold: .08 });
                root.classList.add('motion-ready');
                root.querySelectorAll('[data-reveal]').forEach(element => observer!.observe(element));
            }
            const booking = root.querySelector('#booking');
            const bookingObserver = typeof IntersectionObserver !== 'undefined'
                ? new IntersectionObserver(entries => root.classList.toggle('booking-visible', entries[0].isIntersecting)) : undefined;
            if (booking)
                bookingObserver?.observe(booking);
            window.addEventListener('scroll', onScroll, { passive: true });
            window.addEventListener('resize', onScroll);
            update();
            this.destroy.onDestroy(() => {
                cancelAnimationFrame(frame);
                observer?.disconnect();
                bookingObserver?.disconnect();
                window.removeEventListener('scroll', onScroll);
                window.removeEventListener('resize', onScroll);
            });
        }));
    }
}
