import { Injectable, computed, signal } from '@angular/core';
import type { StaffIdentity } from './models/staff-identity.model';
@Injectable({ providedIn: 'root' })
export class AuthSession {
    readonly identity = signal<StaffIdentity | null>(null);
    readonly username = computed(() => this.identity()?.username || '');
    csrfToken = '';
    clear() {
        this.csrfToken = '';
        this.identity.set(null);
    }
}
