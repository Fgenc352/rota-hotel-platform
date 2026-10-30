import { Injectable, inject, signal } from '@angular/core';
import { AuthSession } from '@core/auth/auth-session.service';
import { HttpApi } from '@core/http/http-api.service';
import { ApiError } from '@core/http/api-error';
import type { StaffIdentity } from './models/staff-identity.model';
@Injectable({ providedIn: 'root' })
export class AuthApi {
    private readonly session = inject(AuthSession);
    private readonly http = inject(HttpApi);
    private restoration: Promise<boolean> | undefined;
    readonly username = this.session.username;
    readonly identity = this.session.identity;
    readonly connectionError = signal('');
    async login(username: string, password: string) {
        this.connectionError.set('');
        this.session.clear();
        try {
            await this.http.request<StaffIdentity>('/api/auth/me', 'GET', undefined, {
                Authorization: 'Basic ' + btoa(username.trim() + ':' + password)
            });
            if (!this.session.csrfToken) {
                throw new ApiError('Backend oturum sürümü güncel değil. Spring Boot uygulamasını yeniden başlatın.', 'SESSION_UNAVAILABLE', '');
            }
            // Verify the cookie before treating login as successful; no Basic header on this request.
            try {
                const me = await this.http.request<StaffIdentity>('/api/auth/me');
                this.identity.set(me);
                return me;
            }
            catch {
                throw new ApiError('Giriş doğrulandı fakat oturum kurulamadı. Spring Boot ve Angular sunucularını yeniden başlatın; tarayıcıda cookie engeli olmadığını kontrol edin.', 'SESSION_UNAVAILABLE', '');
            }
        }
        catch (error) {
            this.session.clear();
            throw error;
        }
    }
    restore(): Promise<boolean> {
        if (this.identity()) {
            return Promise.resolve(true);
        }
        this.restoration ??= this.http.request<StaffIdentity>('/api/auth/me')
            .then(me => {
            this.connectionError.set('');
            this.identity.set(me);
            return true;
        })
            .catch(error => {
            this.session.clear();
            this.connectionError.set(error instanceof ApiError && error.status === 401
                ? '' : 'Oturum doğrulanamadı. Sunucunun çalıştığını ve bağlantınızı kontrol edin.');
            return false;
        })
            .finally(() => {
            this.restoration = undefined;
        });
        return this.restoration;
    }
    async logout() {
        await this.http.request('/api/auth/logout', 'POST');
        this.session.clear();
    }
}
