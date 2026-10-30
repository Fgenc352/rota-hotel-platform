import { test, expect } from '@playwright/test';
import { roomFor, mockBooking } from './fixtures/showcase-api.mjs';
for (const key of ['lagos-beach', 'peri-stone']) {
    test(`${key}: new hotel page shows prices and a keyboard-dismissable photo/detail gallery`, async ({ page }) => {
        await mockBooking(page);
        const room = roomFor(key);
        await page.goto('/showcase/' + key);
        await expect(page.getByRole('heading', { name: room.name, exact: true })).toBeVisible();
        await expect(page.locator('.room-price strong').first()).toContainText(key === 'peri-stone' ? '5.500' : '3.200');
        await expect(page.locator('.hero-image, .arch-frame img').first()).toBeVisible();
        await expect.poll(() => page.locator('.hero-image, .arch-frame img').first().evaluate(image => image.naturalWidth)).toBeGreaterThan(0);
        await page.getByRole('button', { name: /Tüm özellikler ve fotoğraflar/ }).click();
        const dialog = page.getByRole('dialog');
        await expect(dialog).toBeVisible();
        await expect(dialog.getByText('1 çift kişilik yatak', { exact: true })).toBeVisible();
        await expect(dialog.getByText('Kahve köşesi', { exact: false })).toBeVisible();
        await dialog.getByRole('button', { name: 'Fotoğraf 2', exact: true }).click();
        await expect(dialog.locator('.large-image')).toHaveAttribute('src', room.images[1]);
        await page.keyboard.press('Escape');
        await expect(dialog).toHaveCount(0);
        await expect(page.getByRole('button', { name: /Tüm özellikler ve fotoğraflar/ })).toBeFocused();
    });
}
test('premium widget completes a real-shaped booking through the existing public contract', async ({ page }) => {
    const { bookings } = await mockBooking(page);
    await page.goto('/widget-premium?hotel=lagos-beach');
    await page.getByLabel('Giriş tarihi', { exact: true }).fill('2027-02-10');
    await page.getByLabel('Çıkış tarihi', { exact: true }).fill('2027-02-12');
    await page.getByRole('button', { name: 'Oda ara', exact: true }).click();
    await expect(page.getByText('3 oda müsait', { exact: false })).toBeVisible();
    await page.getByRole('button', { name: /Odayı seç/ }).click();
    await page.getByLabel('Ad', { exact: true }).fill('Deniz');
    await page.getByLabel('Soyad', { exact: true }).fill('Test');
    await page.getByLabel('E-posta', { exact: true }).fill('showcase@example.com');
    await page.getByLabel('Telefon (ülke koduyla)', { exact: true }).fill('+905550000099');
    await page.getByLabel('KVKK aydınlatma metnini okudum.').check();
    await page.getByRole('button', { name: 'Rezervasyonu tamamla', exact: true }).click();
    await expect(page.locator('.booking-code')).toHaveText('RT-SHOWCASE-01');
    expect(bookings).toHaveLength(1);
    expect(bookings[0].path).toBe('/api/public/hotels/lagos-beach/reservations');
    expect(bookings[0].body.roomTypeId).toBe(roomFor('lagos-beach').id);
    expect(bookings[0].headers['idempotency-key']).toBeTruthy();
});
test('a preferred room is carried from the hotel page to its scoped widget', async ({ page }) => {
    await mockBooking(page);
    await page.goto('/showcase/peri-stone');
    await page.getByRole('button', { name: /Tarih seç/ }).click();
    const widget = page.frameLocator('iframe');
    await expect(widget.locator('.preferred-room')).toContainText('Mağara Oda');
    await widget.getByRole('button', { name: 'Oda ara', exact: true }).click();
    await expect(widget.locator('.selected-stay')).toContainText('Mağara Oda');
});
test('empty availability does not invent rooms or let a preferred room bypass stock', async ({ page }) => {
    const { bookings } = await mockBooking(page, { empty: true });
    await page.goto('/widget-premium?hotel=peri-stone&room=' + roomFor('peri-stone').id);
    await page.getByRole('button', { name: 'Oda ara', exact: true }).click();
    await expect(page.getByText('Bu tarihlerde kişi sayınıza uygun oda bulunamadı.', { exact: false })).toBeVisible();
    await expect(page.getByRole('button', { name: /Odayı seç/ })).toHaveCount(0);
    expect(bookings).toEqual([]);
});
test('showcase pages fit a narrow viewport and retired hotel routes lead to signature', async ({ page }) => {
    await mockBooking(page);
    await page.setViewportSize({ width: 390, height: 844 });
    for (const key of ['lagos-beach', 'peri-stone']) {
        await page.goto('/showcase/' + key);
        await expect(page.locator('h1').first()).toBeVisible();
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
        await page.goto('/hotels/' + key);
        await expect(page).toHaveURL('/signature/' + key);
        await expect(page.locator('showcase-booking-embed iframe')).toHaveAttribute('src', '/widget-signature?hotel=' + key);
    }
});
