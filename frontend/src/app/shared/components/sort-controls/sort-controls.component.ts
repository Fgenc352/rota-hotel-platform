import { Component, input, output } from '@angular/core';
import type { SortOption, SortSelection } from '../../models/sort.model';
import { IconComponent } from '../icon/icon.component';
@Component({
    selector: 'ui-sort-controls', standalone: true, imports: [IconComponent],
    templateUrl: './sort-controls.component.html', styleUrl: './sort-controls.component.css'
})
export class SortControlsComponent {
    readonly value = input.required<SortSelection>();
    readonly options = input.required<SortOption[]>();
    readonly busy = input(false);
    readonly changed = output<SortSelection>();
    field(event: Event) {
        this.changed.emit({ ...this.value(), field: (event.target as HTMLSelectElement).value });
    }
    direction() {
        this.changed.emit({ ...this.value(), direction: this.value().direction === 'ASC' ? 'DESC' : 'ASC' });
    }
}
