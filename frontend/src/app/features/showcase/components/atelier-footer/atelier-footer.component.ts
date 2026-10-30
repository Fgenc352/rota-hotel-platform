import { Component, input } from '@angular/core';
import { BookingScrollDirective } from '../../directives/booking-scroll.directive';
import { IconComponent } from '@shared/components/icon/icon.component';
@Component({
    selector: 'atelier-site-footer', standalone: true, imports: [BookingScrollDirective, IconComponent],
    templateUrl: './atelier-footer.component.html', styleUrl: './atelier-footer.component.css'
})
export class AtelierFooterComponent {
    readonly cave = input(false);
    readonly year = new Date().getFullYear();
}
