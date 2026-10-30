import { test, expect } from '@playwright/test';
const sourceId = '30000000-0000-0000-0000-000000000001';
const source = {
    id: sourceId, first_name: 'Mehmed', last_name: 'Yılmaz', email: 'first@example.com',
    phone: '+905550000002', marketing_allowed: false,
    stays: 0, spend: 0, active_reservations: 1, booked_value: 10
};
async function mockGuests(page, initialNotes = [], contacts = { emails: [source.email], phones: [source.phone] }) {
    let notes = [...initialNotes];
    const deletions = [];
    const searches = [];
    await page.route('**/api/**', async (route) => {
        const url = new URL(route.request().url());
        let body;
        if (url.pathname === '/api/auth/me') {
            body = {
                username: 'lagos', hotelId: 'hotel', hotelKey: 'lagos-beach', hotelName: 'Lagos Beach Hotel', location: 'Antalya'
            };
        }
        else if (url.pathname === '/api/crm/segments') {
            body = [];
        }
        else if (route.request().method() === 'DELETE' && /\/notes\/[^/]+$/.test(url.pathname)) {
            const id = url.pathname.split('/').at(-1);
            deletions.push(id);
            notes = notes.filter(note => note.id !== id);
            await route.fulfill({ status: 204 });
            return;
        }
        else if (url.pathname === '/api/crm/guests') {
            searches.push(Object.fromEntries(url.searchParams));
            body = {
                items: [source], total: 1, page: 0, size: 20
            };
        }
        else {
            body = {
                guest: source, reservations: [], notes, tags: [], ...contacts
            };
        }
        await route.fulfill({
            status: 200, json: body, headers: { 'X-CSRF-TOKEN': 'test-csrf' }
        });
    });
    return { searches, deletions };
}
test('independent guest filters are combined and clearing resets them', async ({ page }) => {
    const { searches } = await mockGuests(page);
    await page.goto('/crm/guests');
    await page.getByLabel('Ad', { exact: true }).fill('Mehmed');
    await page.getByLabel('Soyad', { exact: true }).fill('Yılmaz');
    await page.getByLabel('E-posta', { exact: true }).fill('first@example.com');
    await page.getByLabel('Telefon', { exact: true }).fill('000002');
    await page.getByText('Konaklama tarihi filtreleri', { exact: false }).click();
    const arrival = page.getByRole('group', { name: 'Giriş tarihi', exact: true });
    const departure = page.getByRole('group', { name: 'Çıkış tarihi', exact: true });
    await arrival.getByLabel('Başlangıç').fill('2027-03-01');
    await departure.getByLabel('Bitiş').fill('2027-03-05');
    await page.getByRole('button', { name: 'Filtrele', exact: true }).click();
    await expect.poll(() => searches.at(-1)).toMatchObject({
        firstName: 'Mehmed', lastName: 'Yılmaz', email: 'first@example.com', phone: '000002',
        checkInFrom: '2027-03-01', checkOutTo: '2027-03-05', page: '0'
    });
    await page.getByRole('button', { name: 'Temizle', exact: true }).click();
    await expect.poll(() => searches.at(-1).firstName).toBeUndefined();
    await expect(page.getByLabel('E-posta', { exact: true })).toHaveValue('');
});
test('the profile shows all contact aliases without staff merge actions', async ({ page }) => {
    const contacts = { emails: [source.email, 'additional@example.com'], phones: [source.phone, '+905550000099'] };
    await mockGuests(page, [], contacts);
    await page.goto('/crm/guests');
    await page.getByRole('button', { name: 'Profili aç' }).click();
    await expect(page.getByRole('link', { name: 'additional@example.com', exact: true })).toBeVisible();
    await expect(page.getByRole('link', { name: '+905550000099', exact: true })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Misafir eşleştirme incelemesi' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Bu profili seç', exact: true })).toHaveCount(0);
});

test('a fresh note needs confirmation and disappears after successful deletion', async ({ page }) => {
    const note = { id: '40000000-0000-0000-0000-000000000001', author: 'lagos', body: 'Yanlış yazılan not',
        created_at: new Date(Date.now() - 3600000).toISOString() };
    const { deletions } = await mockGuests(page, [note]);
    await page.goto('/crm/guests');
    await page.getByRole('button', { name: 'Profili aç' }).click();
    const article = page.locator('.note').filter({ hasText: note.body });
    await expect(article.getByRole('button', { name: 'Notu sil', exact: true })).toBeEnabled();
    await article.getByRole('button', { name: 'Notu sil', exact: true }).click();
    expect(deletions).toEqual([]);
    await article.getByRole('button', { name: 'Vazgeç', exact: true }).click();
    expect(deletions).toEqual([]);
    await article.getByRole('button', { name: 'Notu sil', exact: true }).click();
    await article.getByRole('button', { name: 'Evet, notu sil', exact: true }).click();
    await expect(article).toHaveCount(0);
    expect(deletions).toEqual([note.id]);
});

test("expired notes and another staff member's notes cannot be deleted from the profile", async ({ page }) => {
    const notes = [
        { id: '40000000-0000-0000-0000-000000000001', author: 'lagos', body: 'Geçmiş not',
            created_at: new Date(Date.now() - 25 * 3600000).toISOString() },
        { id: '40000000-0000-0000-0000-000000000002', author: 'other-staff', body: 'Başka personelin notu',
            created_at: new Date().toISOString() }
    ];
    const { deletions } = await mockGuests(page, notes);
    await page.goto('/crm/guests');
    await page.getByRole('button', { name: 'Profili aç' }).click();
    for (const note of notes) {
        const article = page.locator('.note').filter({ hasText: note.body });
        await expect(article.getByRole('button', { name: 'Notu sil', exact: true })).toBeDisabled();
    }
    await expect(page.getByText('24 saatlik silme süresi doldu.', { exact: true })).toBeVisible();
    expect(deletions).toEqual([]);
});

test('a note expires while the profile stays open', async ({ page }) => {
    const now = Date.now();
    await page.clock.install({ time: now });
    const note = { id: '40000000-0000-0000-0000-000000000001', author: 'lagos', body: 'Süresi dolacak not',
        created_at: new Date(now - 24 * 3600000 + 120000).toISOString() };
    const { deletions } = await mockGuests(page, [note]);
    await page.goto('/crm/guests');
    await page.getByRole('button', { name: 'Profili aç' }).click();
    const button = page.locator('.note').getByRole('button', { name: 'Notu sil', exact: true });
    await expect(button).toBeEnabled();
    await page.clock.runFor(121000);
    await expect(button).toBeDisabled();
    expect(deletions).toEqual([]);
});

test('custom segments use stays, months and consent without an optional checkout-day field', async ({ page }) => {
    await mockGuests(page);
    const requests = [];
    await page.route('**/api/crm/segments', async route => {
        if (route.request().method() === 'POST') {
            requests.push(route.request().postDataJSON());
            await route.fulfill({ status: 200, json: { id: '50000000-0000-0000-0000-000000000001' } });
        } else {
            await route.fulfill({ status: 200, json: [] });
        }
    });
    await page.goto('/crm/guests');
    await page.getByText('Segment yönetimi', { exact: true }).click();
    await page.getByRole('button', { name: /Segment oluştur/ }).click();
    await expect(page.getByLabel(/Kaç gün önce çıkış/)).toHaveCount(0);
    await page.getByLabel('Segment adı', { exact: true }).fill('Tekrar gelenler');
    await page.getByLabel('Pazarlama izni zorunlu olsun').check();
    await page.getByRole('button', { name: 'Segmenti kaydet', exact: true }).click();
    await expect.poll(() => requests).toEqual([{ name: 'Tekrar gelenler', minStays: 2, months: 12, marketingRequired: true }]);
});
