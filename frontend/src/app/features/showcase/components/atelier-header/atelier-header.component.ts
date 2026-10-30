import { Component, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconComponent } from '@shared/components/icon/icon.component';
import { BookingScrollDirective } from '../../directives/booking-scroll.directive';
@Component({
    selector: 'atelier-site-header', standalone: true,
    imports: [RouterLink, IconComponent, BookingScrollDirective],
    templateUrl: './atelier-header.component.html', styleUrl: './atelier-header.component.css'
})
export class AtelierHeaderComponent {
    readonly menu = signal(false);
    readonly cave = input(false);
}
