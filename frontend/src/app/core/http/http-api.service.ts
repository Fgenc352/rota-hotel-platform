import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { ApiError } from './api-error';
/** HTTP transport only. Session credentials, CSRF and expiry are handled by the session interceptor. */
@Injectable({ providedIn: 'root' })
export class HttpApi {
    private readonly http = inject(HttpClient);
    async request<T>(path: string, method = 'GET', body?: unknown, headers: Record<string, string> = {}): Promise<T> {
        try {
            return await firstValueFrom(this.http.request<T>(method, path, {
                body,
                headers,
                timeout: 15000,
                credentials: 'omit'
            }));
        }
        catch (error) {
            if (!(error instanceof HttpErrorResponse)) {
                throw error;
            }
            const problem = error.error && typeof error.error === 'object' ? error.error : {};
            throw new ApiError(problem.detail || (error.status === 0 ? 'Sunucuya ulaşılamadı. Bağlantınızı kontrol edin.' : 'İşlem tamamlanamadı.'), problem.code || (error.status === 0 ? 'NETWORK_ERROR' : 'REQUEST_FAILED'), problem.traceId || '', error.status);
        }
    }
}
