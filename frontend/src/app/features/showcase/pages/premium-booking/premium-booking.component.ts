import { StayDatesComponent } from '@shared/components/stay-dates/stay-dates.component';
import { Component, inject, OnInit, ViewEncapsulation } from '@angular/core';
import { BookingStore } from '@booking/state/booking.store';
import { BookingPresentation } from '@booking/presentation/booking-presentation.service';
import { StaySearchComponent } from '@booking/components/stay-search/stay-search.component';
import { BookingGuestFormComponent } from '@booking/components/booking-guest-form/booking-guest-form.component';
import { BookingConfirmationComponent } from '@booking/components/booking-confirmation/booking-confirmation.component';
import { RoomCollectionComponent } from '../../components/room-collection/room-collection.component';
import { BookingStepsComponent } from '../../components/booking-steps/booking-steps.component';
import { WidgetResizeDirective } from '../../components/widget-resize/widget-resize.directive';
import { BookingNavigation } from '../../state/booking-navigation.service';
import { BookingExperience } from '../../state/booking-experience.service';
@Component({
    standalone: true,
    imports: [
        StayDatesComponent, StaySearchComponent, BookingGuestFormComponent, BookingConfirmationComponent,
        RoomCollectionComponent, BookingStepsComponent, WidgetResizeDirective
    ],
    providers: [
        BookingPresentation, BookingStore, BookingNavigation, BookingExperience
    ],
    encapsulation: ViewEncapsulation.None,
    templateUrl: './premium-booking.component.html',
    styleUrl: './premium-booking.component.css'
})
export class PremiumBookingComponent implements OnInit {
    readonly experience = inject(BookingExperience);
    readonly booking = this.experience.booking;
    readonly journey = this.experience.journey;
    ngOnInit() {
        this.experience.initialize();
    }
}
