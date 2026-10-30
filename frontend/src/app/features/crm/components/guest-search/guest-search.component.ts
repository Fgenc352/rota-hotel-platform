import { Component, input, output } from '@angular/core';
import { IconComponent } from '@shared/components/icon/icon.component';
import { FormsModule } from '@angular/forms';
import { emptyGuestSearch } from '../../models/guest-search.model';
import type { GuestSearch } from '../../models/guest-search.model';
@Component({
    selector: 'crm-guest-search',
    standalone: true,
    imports: [FormsModule, IconComponent],
    templateUrl: './guest-search.component.html',
    styleUrl: './guest-search.component.css'
})
export class GuestSearchComponent {
    readonly busy = input(false);
    readonly value = input.required<GuestSearch>();
    readonly searched = output<GuestSearch>();
    filters = emptyGuestSearch();
    ngOnChanges() {
        this.filters = { ...this.value() };
    }
    submit() {
        this.searched.emit({ ...this.filters });
    }
    reset() {
        this.filters = emptyGuestSearch();
        this.submit();
    }
}
