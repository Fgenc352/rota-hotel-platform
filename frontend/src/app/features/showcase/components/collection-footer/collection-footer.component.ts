import { Component, input } from '@angular/core';
import { BookingScrollDirective } from '../../directives/booking-scroll.directive';
import { IconComponent } from '@shared/components/icon/icon.component';
@Component({
    selector: 'collection-footer', standalone: true, imports: [BookingScrollDirective, IconComponent],
    templateUrl: './collection-footer.component.html', styleUrl: './collection-footer.component.css'
})
export class CollectionFooterComponent {
    readonly cave = input(false);
    readonly edition = input<'maison' | 'quiet'>('maison');
}
