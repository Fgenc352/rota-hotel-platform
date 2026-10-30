import { bookingWidgetOffset } from '@shared/utils/booking-widget-scroll';
import { inject, provideAppInitializer, type ApplicationConfig } from '@angular/core';
import { DOCUMENT, ViewportScroller } from '@angular/common';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { sessionInterceptor } from '@core/auth/session.interceptor';
import { routes } from './app.routes';
export const appConfig: ApplicationConfig = {
    providers: [
        provideAppInitializer(() => {
            const scroller = inject(ViewportScroller);
            const document = inject(DOCUMENT);
            scroller.setOffset(() => {
                const header = document.querySelector('.signature-header, .site-header, .peri-header, .atelier-header, .collection-header');
                const headerHeight = header?.getBoundingClientRect().height || 0;
                const window = document.defaultView;
                if (window?.location.hash === '#booking') {
                    const frame = document.querySelector('showcase-booking-embed#booking iframe');
                    if (frame)
                        return [0, bookingWidgetOffset(window.innerHeight, headerHeight, frame.getBoundingClientRect().height)];
                }
                return [0, headerHeight + 20];
            });
        }),
        provideRouter(routes, withInMemoryScrolling({ anchorScrolling: 'enabled' })),
        provideHttpClient(withFetch(), withInterceptors([sessionInterceptor]))
    ]
};
