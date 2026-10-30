import { ActivatedRoute } from '@angular/router';
import { Component, computed, inject, signal, ViewEncapsulation } from '@angular/core';
import { BookingStore } from '@booking/state/booking.store';
import { BookingPresentation } from '@booking/presentation/booking-presentation.service';
import { BookingExperience } from '../../state/booking-experience.service';
import { BookingNavigation } from '../../state/booking-navigation.service';
import { WidgetResizeDirective } from '../../components/widget-resize/widget-resize.directive';
import { BookingStepsComponent } from '../../components/booking-steps/booking-steps.component';
import { StaySearchComponent } from '@booking/components/stay-search/stay-search.component';
import { BookingGuestFormComponent } from '@booking/components/booking-guest-form/booking-guest-form.component';
import { BookingConfirmationComponent } from '@booking/components/booking-confirmation/booking-confirmation.component';
import { RoomCollectionComponent } from '../../components/room-collection/room-collection.component';
import { RoomDetailsDialogComponent } from '../../components/room-details-dialog/room-details-dialog.component';
import { AtelierBookingSummaryComponent } from '../../components/atelier-booking-summary/atelier-booking-summary.component';
import { IconComponent } from '@shared/components/icon/icon.component';
@Component({
    standalone: true,
    imports: [
        WidgetResizeDirective, BookingStepsComponent, StaySearchComponent, BookingGuestFormComponent, BookingConfirmationComponent, RoomCollectionComponent, RoomDetailsDialogComponent, AtelierBookingSummaryComponent, IconComponent
    ],
    providers: [
        BookingPresentation, BookingStore, BookingNavigation, BookingExperience
    ],
    encapsulation: ViewEncapsulation.None,
    templateUrl: './collection-booking.component.html', styleUrl: './collection-booking.component.css'
})
export class CollectionBookingComponent {
    readonly edition = inject(ActivatedRoute).snapshot.data['edition'] as 'maison' | 'quiet';
    readonly experience = inject(BookingExperience);
    readonly booking = this.experience.booking;
    readonly journey = this.experience.journey;
    readonly gallery = signal(false);
    readonly hero = computed(() => '/images/atelier/' + (this.experience.cave ? 'peri' : 'lagos') + '-atelier-hero.webp');
    readonly title = computed(() => [
        this.booking.t('Bir güzel yolculuğun ilk adımı.', 'The beginning of a beautiful stay.'),
        this.booking.t('Sizin için, size ait bir oda.', 'Find the room that feels like you.'),
        this.booking.t('Geriye yalnızca siz kaldınız.', 'All that is missing is you.'),
        this.booking.t('Güzel şeyler sizi bekliyor.', 'Something lovely awaits.')
    ][this.journey.step() - 1]);
    ngOnInit() {
        this.experience.initialize();
    }
}
