import { IconComponent } from '@shared/components/icon/icon.component';
import { StayDatesComponent } from '@shared/components/stay-dates/stay-dates.component';
import { Component, computed, inject, OnInit, signal, ViewEncapsulation } from '@angular/core';
import { BookingStore } from '@booking/state/booking.store';
import { BookingPresentation } from '@booking/presentation/booking-presentation.service';
import { StaySearchComponent } from '@booking/components/stay-search/stay-search.component';
import { BookingGuestFormComponent } from '@booking/components/booking-guest-form/booking-guest-form.component';
import { BookingConfirmationComponent } from '@booking/components/booking-confirmation/booking-confirmation.component';
import { BookingExperience } from '../../state/booking-experience.service';
import { BookingNavigation } from '../../state/booking-navigation.service';
import { BookingStepsComponent } from '../../components/booking-steps/booking-steps.component';
import { WidgetResizeDirective } from '../../components/widget-resize/widget-resize.directive';
import { RoomCollectionComponent } from '../../components/room-collection/room-collection.component';
import { RoomDetailsDialogComponent } from '../../components/room-details-dialog/room-details-dialog.component';
@Component({
    standalone: true,
    imports: [
        IconComponent, StayDatesComponent, StaySearchComponent, BookingGuestFormComponent, BookingConfirmationComponent,
        BookingStepsComponent, WidgetResizeDirective, RoomCollectionComponent, RoomDetailsDialogComponent
    ],
    providers: [
        BookingPresentation, BookingStore, BookingNavigation, BookingExperience
    ],
    encapsulation: ViewEncapsulation.None,
    templateUrl: './signature-booking.component.html',
    styleUrl: './signature-booking.component.css'
})
export class SignatureBookingComponent implements OnInit {
    readonly experience = inject(BookingExperience);
    readonly booking = this.experience.booking;
    readonly journey = this.experience.journey;
    readonly gallery = signal(false);
    readonly portrait = computed(() => this.experience.summaryOffer() ? this.experience.selectedPhoto()
        : '/images/showcase/' + (this.experience.cave ? 'peri' : 'lagos') + '-hero.webp');
    readonly headings = computed(() => [
        this.booking.t('Size ait bir zaman ayıralım.', 'Make time for yourself.'),
        this.booking.t('Hangi oda sizin hikâyeniz?', 'Which room tells your story?'),
        this.booking.t('Sizi biraz tanıyalım.', 'Let us get to know you.'),
        this.booking.t('Güzel bir yolculuk başlıyor.', 'A beautiful journey begins.')
    ][this.journey.step() - 1]);
    ngOnInit() {
        this.experience.initialize();
    }
}
