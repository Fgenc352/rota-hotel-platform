import { Injectable, signal } from '@angular/core';
@Injectable()
export class ActionState {
    readonly busy = signal(false);
    readonly error = signal('');
    readonly notice = signal('');
    async run(work: () => Promise<unknown>) {
        if (this.busy()) {
            return;
        }
        this.busy.set(true);
        this.error.set('');
        this.notice.set('');
        try {
            await work();
        }
        catch (e) {
            this.error.set(e instanceof Error ? e.message : 'İşlem tamamlanamadı.');
        }
        finally {
            this.busy.set(false);
        }
    }
}
