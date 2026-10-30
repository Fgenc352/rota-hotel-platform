import { Component, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BookingScrollDirective } from '../../directives/booking-scroll.directive';
import { IconComponent } from '@shared/components/icon/icon.component';
@Component({
    selector: 'collection-header', standalone: true,
    imports: [RouterLink, BookingScrollDirective, IconComponent],
    templateUrl: './collection-header.component.html',
    styleUrl: './collection-header.component.css'
})
export class CollectionHeaderComponent {
    readonly cave = input(false);
    readonly edition = input<'maison' | 'quiet'>('maison');
    readonly menu = signal(false);
}
