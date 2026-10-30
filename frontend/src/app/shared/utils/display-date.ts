function calendar(value?: string | null): Date | null {
    if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value))
        return null;
    const [year, month, day] = value.split('-').map(Number);
    const date = new Date(Date.UTC(year, month - 1, day));
    return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day ? date : null;
}
export function formatCalendarDate(value?: string | null, locale = 'tr-TR'): string {
    const date = calendar(value);
    return date ? new Intl.DateTimeFormat(locale, {
        day: '2-digit', month: 'short', year: 'numeric', timeZone: 'UTC'
    }).format(date) : '—';
}
export function formatHotelTimestamp(value?: string | null, timezone = 'Europe/Istanbul'): string {
    if (!value || !Number.isFinite(Date.parse(value)))
        return '—';
    return new Intl.DateTimeFormat('tr-TR', {
        dateStyle: 'medium', timeStyle: 'short', timeZone: timezone
    }).format(new Date(value));
}
export function stayNights(checkIn: string, checkOut: string): number {
    const start = calendar(checkIn);
    const end = calendar(checkOut);
    return start && end ? Math.max(0, Math.round((end.getTime() - start.getTime()) / 86400000)) : 0;
}
