export function label(status: string) {
    return ({
        PENDING: 'Bekliyor',
        CONFIRMED: 'Onaylandı',
        CHECKED_IN: 'Giriş yapıldı',
        CHECKED_OUT: 'Çıkış yapıldı',
        CANCELLED: 'İptal',
        NO_SHOW: 'Gelmedi'
    } as Record<string, string>)[status] || status;
}
export function allowed(status: string): string[] {
    return ({
        PENDING: ['CONFIRMED', 'CANCELLED'],
        CONFIRMED: ['CHECKED_IN', 'CANCELLED', 'NO_SHOW'],
        CHECKED_IN: ['CHECKED_OUT']
    } as Record<string, string[]>)[status] || [];
}
