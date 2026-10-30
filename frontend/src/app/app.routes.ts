import type { Routes } from '@angular/router';
import { loginGuard, staffGuard } from '@core/auth/staff.guard';
export const routes: Routes = [
    {
        path: 'maison/lagos-beach',
        data: { hotel: 'lagos-beach' },
        loadComponent: () => import('./features/showcase/pages/maison-hotel/maison-hotel.component')
            .then(module => module.MaisonHotelComponent)
    },
    {
        path: 'maison/peri-stone',
        data: { hotel: 'peri-stone' },
        loadComponent: () => import('./features/showcase/pages/maison-hotel/maison-hotel.component')
            .then(module => module.MaisonHotelComponent)
    },
    {
        path: 'widget-maison',
        data: { edition: 'maison' },
        loadComponent: () => import('./features/showcase/pages/collection-booking/collection-booking.component')
            .then(module => module.CollectionBookingComponent)
    },
    {
        path: 'quiet/lagos-beach',
        data: { hotel: 'lagos-beach' },
        loadComponent: () => import('./features/showcase/pages/quiet-hotel/quiet-hotel.component')
            .then(module => module.QuietHotelComponent)
    },
    {
        path: 'quiet/peri-stone',
        data: { hotel: 'peri-stone' },
        loadComponent: () => import('./features/showcase/pages/quiet-hotel/quiet-hotel.component')
            .then(module => module.QuietHotelComponent)
    },
    {
        path: 'widget-quiet',
        data: { edition: 'quiet' },
        loadComponent: () => import('./features/showcase/pages/collection-booking/collection-booking.component')
            .then(module => module.CollectionBookingComponent)
    },
    {
        path: 'atelier/lagos-beach',
        loadComponent: () => import('./features/showcase/pages/lagos-atelier/lagos-atelier.component')
            .then(module => module.LagosAtelierComponent)
    },
    {
        path: 'atelier/peri-stone',
        loadComponent: () => import('./features/showcase/pages/peri-atelier/peri-atelier.component')
            .then(module => module.PeriAtelierComponent)
    },
    {
        path: 'widget-atelier',
        loadComponent: () => import('./features/showcase/pages/atelier-booking/atelier-booking.component')
            .then(module => module.AtelierBookingComponent)
    },
    {
        path: 'room-gallery',
        loadComponent: () => import('./features/showcase/pages/room-gallery/room-gallery.component')
            .then(module => module.RoomGalleryComponent)
    },
    {
        path: 'signature/lagos-beach',
        loadComponent: () => import('./features/showcase/pages/lagos-signature/lagos-signature.component')
            .then(module => module.LagosSignatureComponent)
    },
    {
        path: 'signature/peri-stone',
        loadComponent: () => import('./features/showcase/pages/peri-signature/peri-signature.component')
            .then(module => module.PeriSignatureComponent)
    },
    {
        path: 'widget-signature',
        loadComponent: () => import('./features/showcase/pages/signature-booking/signature-booking.component')
            .then(module => module.SignatureBookingComponent)
    },
    {
        path: 'showcase/lagos-beach',
        loadComponent: () => import('./features/showcase/pages/lagos/lagos-showcase.component')
            .then(module => module.LagosShowcaseComponent)
    },
    {
        path: 'showcase/peri-stone',
        loadComponent: () => import('./features/showcase/pages/peri/peri-showcase.component')
            .then(module => module.PeriShowcaseComponent)
    },
    {
        path: 'widget-premium',
        loadComponent: () => import('./features/showcase/pages/premium-booking/premium-booking.component')
            .then(module => module.PremiumBookingComponent)
    },
    {
        path: 'widget',
        loadComponent: () => import('./features/booking/pages/booking-page/booking-page.component')
            .then(module => module.BookingPageComponent)
    },
    {
        path: 'hotels/lagos-beach', redirectTo: 'signature/lagos-beach', pathMatch: 'full'
    },
    {
        path: 'hotels/peri-stone', redirectTo: 'signature/peri-stone', pathMatch: 'full'
    },
    {
        path: 'hotels', redirectTo: 'signature/lagos-beach', pathMatch: 'full'
    },
    {
        path: 'login',
        title: 'Personel girişi | Rota',
        canActivate: [loginGuard],
        loadComponent: () => import('./features/auth/pages/login/login.component')
            .then(module => module.LoginComponent)
    },
    {
        path: 'crm',
        canActivate: [staffGuard],
        canActivateChild: [staffGuard],
        loadComponent: () => import('./features/crm/pages/crm-shell/crm-shell.component')
            .then(module => module.CrmShellComponent),
        children: [
            {
                path: '', redirectTo: 'guests', pathMatch: 'full'
            },
            {
                path: 'guests',
                data: { title: 'Misafirler' },
                loadComponent: () => import('./features/crm/pages/guests/guest-list.component')
                    .then(module => module.GuestListComponent)
            },
            {
                path: 'reservations',
                data: { title: 'Rezervasyonlar' },
                loadComponent: () => import('./features/crm/pages/reservations/reservations.component')
                    .then(module => module.ReservationsComponent)
            },
            {
                path: 'pms',
                data: { title: 'PMS / Resepsiyon' },
                loadComponent: () => import('./features/pms/pages/pms-page/pms-page.component')
                    .then(module => module.PmsPageComponent)
            },
            {
                path: 'operations',
                data: { title: 'Genel bakış' },
                loadComponent: () => import('./features/crm/pages/operations/operations.component')
                    .then(module => module.OperationsComponent)
            }
        ]
    },
    {
        path: '',
        redirectTo: 'crm',
        pathMatch: 'full'
    },
    {
        path: '**',
        redirectTo: 'crm'
    }
];
