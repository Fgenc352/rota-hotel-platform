import { test, expect } from '@playwright/test';
const guest = {
    id: 'd1000000-0000-0000-0000-000000000001', first_name: 'Deniz', last_name: 'Yılmaz',
    email: 'deniz@example.com', phone: '+905550000001',
    marketing_allowed: true, consent_at: '2027-02-10T22:30:00Z',
    stays: 13, spend: 41600, active_reservations: 14, booked_value: 44800
};
const reservation = {
    id: 'booking-a', reservation_id: 'booking-a', code: 'RT-WORKSPACE',
    first_name: 'Deniz', last_name: 'Yılmaz', room_name: 'Standart',
    guest_id: guest.id, email: guest.email, phone: guest.phone, adults: 2, children: 1, room_total: 6400,
    services: [{
            code: 'BREAKFAST', name: 'Sabah kahvaltısı', unit: 'PER_PERSON_PER_NIGHT', unitPrice: 0, quantity: 6, total: 0, included: true
        }],
    check_in: '2027-02-10', check_out: '2027-02-12', status: 'CONFIRMED', total: 6400
};
async function mockWorkspace(page) {
    const requests = [];
    await page.route('**/api/**', async (route) => {
        const url = new URL(route.request().url());
        requests.push(url);
        let body;
        if (url.pathname === '/api/auth/me')
            body = {
                username: 'lagos', hotelId: 'hotel', hotelKey: 'lagos-beach', hotelName: 'Lagos Beach Hotel', location: 'Antalya'
            };
        else if (url.pathname === '/api/crm/guests')
            body = {
                items: [guest], total: 41, page: 0, size: 20
            };
        else if (url.pathname === '/api/crm/segments' || url.pathname === '/api/crm/events/failed')
            body = [];
        else if (url.pathname === '/api/crm/reservations')
            body = { items: [reservation], total: 1 };
        else if (url.pathname === '/api/crm/reservations/booking-a')
            body = reservation;
        else if (url.pathname.endsWith('/rooms'))
            body = [];
        else if (url.pathname === '/api/crm/analytics')
            body = {
                reservations: 25, funnel: [{ stage: 'SEARCH', sessions: 10 }, { stage: 'CONFIRMED', sessions: 4 }],
                deliveries: [{
                        consumer: 'CRM', pending: 0, dead: 0
                    }]
            };
        else
            body = {
                guest, reservations: [reservation], notes: [{
                        id: 'note-a', body: 'Sessiz oda tercih ediyor.', author: 'lagos', created_at: '2027-02-10T22:30:00Z'
                    }], tags: ['VIP']
            };
        await route.fulfill({
            status: 200, json: body, headers: { 'X-CSRF-TOKEN': 'workspace-csrf' }
        });
    });
    return requests;
}
test('guest directory prioritizes identity; stays and amounts appear only inside the profile', async ({ page }) => {
    await mockWorkspace(page);
    await page.goto('/crm/guests');
    const table = page.getByRole('table', { name: 'Misafir rehberi' });
    await expect(table.getByRole('columnheader', { name: 'Misafir', exact: true })).toBeVisible();
    await expect(table.getByRole('columnheader', { name: 'Telefon', exact: true })).toBeVisible();
    await expect(table.getByRole('columnheader', { name: 'Konaklama', exact: true })).toHaveCount(0);
    await expect(table.getByRole('columnheader', { name: 'Konaklama bedeli', exact: true })).toHaveCount(0);
    await page.getByRole('button', { name: 'Profili aç', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Deniz Yılmaz', exact: true })).toBeVisible();
    await expect(page.getByText('Tamamlanan konaklama', { exact: true })).toBeVisible();
    await expect(page.locator('.profile-stats')).toContainText('13');
    await expect(page.getByRole('heading', { name: 'Misafir notları', exact: true })).toBeVisible();
    await expect(page.getByText('Sessiz oda tercih ediyor.', { exact: true })).toBeVisible();
    await expect(page.locator('ui-stay-dates').first()).toContainText('Giriş');
    await expect(page.locator('ui-stay-dates').first()).toContainText('10 Şub 2027');
    await expect(page.locator('ui-stay-dates').first()).toContainText('Çıkış');
    await expect(page.locator('ui-stay-dates').first()).not.toContainText('→');
});
test('guest sorting is sent to the server and changing sort resets pagination', async ({ page }) => {
    const requests = await mockWorkspace(page);
    await page.goto('/crm/guests');
    await page.getByRole('button', { name: 'Sonraki', exact: true }).click();
    await expect.poll(() => requests.filter(url => url.pathname === '/api/crm/guests').at(-1)?.searchParams.get('page')).toBe('1');
    await page.getByLabel('Sıralama alanı', { exact: true }).selectOption('EMAIL');
    await expect.poll(() => requests.filter(url => url.pathname === '/api/crm/guests').at(-1)?.searchParams.get('sort')).toBe('EMAIL');
    expect(requests.filter(url => url.pathname === '/api/crm/guests').at(-1).searchParams.get('page')).toBe('0');
    await page.getByRole('button', { name: 'Sıralama yönü: Artan', exact: true }).click();
    await expect.poll(() => requests.filter(url => url.pathname === '/api/crm/guests').at(-1)?.searchParams.get('direction')).toBe('DESC');
});
test('reservations show labeled dates and full action labels inside a reservation card', async ({ page }) => {
    const requests = await mockWorkspace(page);
    await page.goto('/crm/reservations');
    await page.getByLabel('Sıralama alanı', { exact: true }).selectOption('CHECK_IN');
    await expect.poll(() => requests.filter(url => url.pathname === '/api/crm/reservations').at(-1)?.searchParams.get('sort')).toBe('CHECK_IN');
    await page.getByRole('button', { name: 'RT-WORKSPACE rezervasyonunu yönet', exact: true }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.getByRole('heading', { name: 'Deniz Yılmaz', exact: true })).toBeVisible();
    await expect(dialog.locator('ui-stay-dates')).toContainText('12 Şub 2027');
    await expect(dialog.getByRole('button', { name: 'Misafirin girişini yap', exact: true })).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(dialog).toHaveCount(0);
});
test('workspace shows real aggregate values and stays usable on a phone', async ({ page }) => {
    await mockWorkspace(page);
    await page.goto('/crm/operations');
    await expect(page.locator('.stats-grid')).toContainText('25');
    await expect(page.locator('.stats-grid')).toContainText('%40');
    await page.setViewportSize({ width: 390, height: 844 });
    await page.getByRole('link', { name: 'Misafirler', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Çıkış yap', exact: true })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});
test('reservation detail links to its guest and guest history links back to the reservation', async ({ page }) => {
    await mockWorkspace(page);
    await page.goto('/crm/reservations');
    await page.getByRole('button', { name: 'RT-WORKSPACE rezervasyonunu yönet' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog.getByText('deniz@example.com', { exact: true })).toBeVisible();
    await expect(dialog.locator('.occupancy-summary')).toContainText('Çocuk');
    await expect(dialog.getByText('Sabah kahvaltısı', { exact: true })).toBeVisible();
    await dialog.getByRole('link', { name: /Misafir profilini aç/ }).click();
    await expect(page).toHaveURL(/crm\/guests\?guestId=/);
    await expect(page.getByRole('heading', { name: 'Deniz Yılmaz', exact: true })).toBeVisible();
    await page.getByRole('link', { name: 'RT-WORKSPACE rezervasyon detayını aç' }).click();
    await expect(page).toHaveURL(/crm\/reservations\?reservationId=booking-a/);
    await expect(page.getByRole('dialog')).toContainText('Standart');
});
test('reservation filters are sent with sorting and reset the current page', async ({ page }) => {
    const requests = await mockWorkspace(page);
    await page.goto('/crm/reservations');
    await page.getByLabel('Rezervasyon kodu', { exact: true }).fill('RT-WORKSPACE');
    await page.getByLabel('Ad', { exact: true }).fill('Deniz');
    await page.getByLabel('Durum', { exact: true }).selectOption('CONFIRMED');
    await page.getByText('Tarih, iletişim, kişi sayısı ve tutar filtreleri', { exact: true }).click();
    await page.getByLabel('Çocuk sayısı', { exact: true }).fill('0');
    await page.getByRole('button', { name: 'Filtrele', exact: true }).click();
    await expect.poll(() => requests.filter(url => url.pathname === '/api/crm/reservations').at(-1)?.searchParams.get('children')).toBe('0');
    const search = requests.filter(url => url.pathname === '/api/crm/reservations').at(-1).searchParams;
    expect(search.get('firstName')).toBe('Deniz');
    expect(search.get('status')).toBe('CONFIRMED');
    expect(search.get('page')).toBe('0');
    await page.getByRole('button', { name: 'Filtreleri temizle', exact: true }).click();
    await expect.poll(() => requests.filter(url => url.pathname === '/api/crm/reservations').at(-1)?.searchParams.has('children')).toBe(false);
});
