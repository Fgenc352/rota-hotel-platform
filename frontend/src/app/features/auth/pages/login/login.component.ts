import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { IconComponent } from '@shared/components/icon/icon.component';
import { AuthApi } from '@core/auth/auth-api.service';
import { ActionState } from '@shared/state/action-state.service';
@Component({
    selector: 'crm-login',
    standalone: true,
    providers: [ActionState],
    imports: [ReactiveFormsModule, IconComponent],
    styleUrl: './login.component.css',
    templateUrl: './login.component.html'
})
export class LoginComponent {
    private readonly api = inject(AuthApi);
    private readonly router = inject(Router);
    private readonly route = inject(ActivatedRoute);
    readonly connectionError = this.api.connectionError;
    readonly actions = inject(ActionState);
    readonly busy = this.actions.busy;
    readonly error = this.actions.error;
    private readonly fb = inject(FormBuilder);
    readonly loginForm = this.fb.nonNullable.group({
        username: ['', Validators.required],
        password: ['', Validators.required]
    });
    readonly passwordVisible = signal(false);
    async login() {
        this.loginForm.markAllAsTouched();
        if (this.loginForm.invalid) {
            return;
        }
        await this.actions.run(async () => {
            const { username, password } = this.loginForm.getRawValue();
            await this.api.login(username, password);
            this.loginForm.controls.password.setValue('');
            const requested = this.route.snapshot.queryParamMap.get('returnUrl');
            const destinations = [
                '/crm/guests', '/crm/reservations', '/crm/operations', '/crm/pms'
            ];
            const destination = requested && destinations.includes(requested) ? requested : '/crm/guests';
            await this.router.navigateByUrl(destination, { replaceUrl: true });
        });
    }
}
