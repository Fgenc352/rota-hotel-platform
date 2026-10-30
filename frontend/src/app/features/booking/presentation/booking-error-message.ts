import { ApiError } from '@core/http/api-error';
import type { BookingLanguage } from '../models/booking-preferences.model';
export function bookingErrorMessage(e: unknown, language: BookingLanguage) {
    const translate = (tr: string, en: string) => language === 'tr' ? tr : en;
    if (language === 'en' && e instanceof ApiError) {
        const messages: Record<string, string> = {
            VALIDATION: 'Check dates, party size and required fields. Stays must be within one year and at most 30 nights.',
            SOLD_OUT: 'This room has just sold out. Please search again.',
            IDEMPOTENCY_CONFLICT: 'This request key was used for different booking details.',
            NOT_FOUND: 'Hotel or room could not be found.',
            INTERNAL_ERROR: 'The booking could not be completed. Please contact support with the trace ID.'
        };
        return (messages[e.code] || 'The request could not be completed. Please retry.') + (e.traceId ? ' [' + e.traceId + ']' : '');
    }
    if (e instanceof DOMException) {
        return translate('Bağlantı zaman aşımına uğradı. Aynı rezervasyon denemesini yeniden gönderebilirsiniz.', 'Connection timed out. You can retry the same booking safely.');
    }
    return e instanceof Error ? e.message : translate('Beklenmeyen hata.', 'Unexpected error.');
}
