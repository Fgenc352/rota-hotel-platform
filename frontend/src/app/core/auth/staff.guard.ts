import { inject } from '@angular/core';
import { Router, type CanActivateFn } from '@angular/router';
import { AuthApi } from './auth-api.service';
export const staffGuard: CanActivateFn = async (_route, state) => {
    const auth = inject(AuthApi);
    const router = inject(Router);
    return await auth.restore()
        ? true
        : router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};
export const loginGuard: CanActivateFn = async () => {
    const auth = inject(AuthApi);
    const router = inject(Router);
    return await auth.restore() ? router.createUrlTree(['/crm/guests']) : true;
};
