import { Component, effect, inject, signal, ViewEncapsulation } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Title } from '@angular/platform-browser';
import { IconComponent } from '@shared/components/icon/icon.component';
import { AuthApi } from '@core/auth/auth-api.service';
@Component({
    standalone: true,
    imports: [
        RouterLink, RouterLinkActive, RouterOutlet, IconComponent
    ],
    encapsulation: ViewEncapsulation.None,
    styleUrls: ['../../styles/crm-theme.css', './crm-shell.component.css'],
    templateUrl: './crm-shell.component.html'
})
export class CrmShellComponent {
    readonly api = inject(AuthApi);
    private readonly router = inject(Router);
    private readonly title = inject(Title);
    readonly logoutError = signal('');
    constructor() {
        effect(() => {
            this.title.setTitle((this.api.identity()?.hotelName || 'Rota') + ' | Otel Yönetimi');
        });
    }
    async logout() {
        this.logoutError.set('');
        try {
            await this.api.logout();
            await this.router.navigate(['/login'], { replaceUrl: true });
        }
        catch {
            this.logoutError.set('Çıkış yapılamadı. Bağlantınızı kontrol edip tekrar deneyin.');
        }
    }
}
