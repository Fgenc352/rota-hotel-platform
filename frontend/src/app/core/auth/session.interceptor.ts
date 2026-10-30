import { inject } from '@angular/core';
import { HttpErrorResponse, HttpResponse, type HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs';
import { AuthSession } from './auth-session.service';
export const sessionInterceptor: HttpInterceptorFn = (request, next) => {
    const session = inject(AuthSession);
    const router = inject(Router);
    const authenticated = request.url.startsWith('/api/crm/') || request.url.startsWith('/api/auth/');
    if (!authenticated) {
        return next(request);
    }
    const changesState = !['GET', 'HEAD', 'OPTIONS'].includes(request.method);
    const protectedRequest = request.clone({
        credentials: 'same-origin',
        setHeaders: changesState && session.csrfToken ? { 'X-CSRF-TOKEN': session.csrfToken } : {}
    });
    return next(protectedRequest).pipe(tap({
        next: response => {
            if (response instanceof HttpResponse) {
                const token = response.headers.get('X-CSRF-TOKEN');
                if (token) {
                    session.csrfToken = token;
                }
            }
        },
        error: error => {
            if (error instanceof HttpErrorResponse && error.status === 401) {
                session.clear();
                if (request.url.startsWith('/api/crm/')) {
                    void router.navigate(['/login'], {
                        queryParams: { returnUrl: router.url }, replaceUrl: true
                    });
                }
            }
        }
    }));
};
