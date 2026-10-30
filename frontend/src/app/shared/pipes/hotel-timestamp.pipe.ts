import { Pipe, PipeTransform } from '@angular/core';
import { formatHotelTimestamp } from '../utils/display-date';
@Pipe({ name: 'hotelTimestamp', standalone: true })
export class HotelTimestampPipe implements PipeTransform {
    transform(value?: string | null, timezone = 'Europe/Istanbul') {
        return formatHotelTimestamp(value, timezone);
    }
}
