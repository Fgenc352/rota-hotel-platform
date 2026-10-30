import { test, expect } from '@playwright/test';
import { roomFor, mockBooking } from './fixtures/showcase-api.mjs';
for (const key of ['lagos-beach', 'peri-stone']) {
    for (const mobile of [false, true]) {
        test(`${key}: atelier has a separate hotel design, working gallery and its own widget (${mobile ? 'mobile' : 'desktop'})`, async ({ page }) => {
            await mockBooking(page);
            await page.setViewportSize(mobile ? { width: 390, height: 844 } : { width: 1440, height: 1000 });
            await page.goto('/atelier/' + key);
            await expect(page.locator('.atelier-site')).toBeVisible();
            await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
            await expect(page.locator('showcase-booking-embed > iframe')).toHaveAttribute('src', '/widget-atelier?hotel=' + key);
            if (mobile) {
                await page.getByRole('button', { name: 'Otel menüsünü aç veya kapat' }).click();
                await page.getByRole('navigation', { name: 'Otel menüsü' }).getByRole('link', { name: 'Odalar & süitler', exact: true }).click();
            }
            await page.locator('.room-gallery-trigger').click();
            const dialog = page.getByRole('dialog');
            await expect(dialog).toBeVisible();
            await expect(dialog.locator('.thumbnails button')).toHaveCount(5);
            await dialog.getByRole('button', { name: 'Fotoğraf 5', exact: true }).click();
            await expect(dialog.locator('.large-image')).toHaveAttribute('src', roomFor(key).images[4]);
            await dialog.getByRole('button', { name: 'Oda detayını kapat', exact: true }).click();
            await page.getByRole('button', { name: 'Bu odada konakla', exact: true }).click();
            await expect(page.locator('showcase-booking-embed > iframe')).toHaveAttribute('src', '/widget-atelier?hotel=' + key + '&room=' + roomFor(key).id);
            const frame = page.frameLocator('showcase-booking-embed > iframe');
            await expect(frame.locator('.atelier-widget')).toBeVisible();
            expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
        });
    }
    test(`${key}: atelier widget retains guest inputs, translates content and books the selected room and extras`, async ({ page }) => {
        const { bookings } = await mockBooking(page);
        await page.goto('/widget-atelier?hotel=' + key);
        await page.getByRole('button', { name: 'Oda ara', exact: true }).click();
        await page.getByRole('button', { name: 'Odayı seç', exact: true }).click();
        await page.getByLabel('Ad', { exact: true }).fill('Deniz');
        await page.getByLabel('Soyad', { exact: true }).fill('Atelier');
        await page.getByLabel('E-posta', { exact: true }).fill('atelier@example.com');
        await page.getByLabel('Telefon (ülke koduyla)', { exact: true }).fill('+905551234567');
        await page.getByRole('checkbox', { name: 'Vale ve otopark', exact: true }).check();
        await page.getByRole('button', { name: /Odanız/ }).click();
        await page.getByRole('button', { name: 'Odayı seç', exact: true }).click();
        await expect(page.getByLabel('Ad', { exact: true })).toHaveValue('Deniz');
        await page.getByRole('button', { name: 'Change language', exact: true }).click();
        await expect(page.getByRole('checkbox', { name: 'Valet and parking', exact: true })).toBeChecked();
        await page.getByRole('button', { name: 'Change language', exact: true }).click();
        await page.getByRole('checkbox', { name: /KVKK/ }).check();
        await page.getByRole('button', { name: 'Rezervasyonu tamamla', exact: true }).click();
        await expect(page.locator('.booking-code')).toHaveText('RT-SHOWCASE-01');
        expect(bookings).toHaveLength(1);
        expect(bookings[0].path).toBe('/api/public/hotels/' + key + '/reservations');
        expect(bookings[0].body.roomTypeId).toBe(roomFor(key).id);
        expect(bookings[0].body.serviceCodes).toEqual(['VALET']);
        expect(bookings[0].headers['idempotency-key']).toBeTruthy();
    });
    test(`${key}: the retired hotel URL opens the existing signature page`, async ({ page }) => {
        await mockBooking(page);
        await page.goto('/hotels/' + key);
        await expect(page).toHaveURL('/signature/' + key);
        await expect(page.locator('.signature-page')).toBeVisible();
        await expect(page.locator('showcase-booking-embed > iframe')).toHaveAttribute('src', '/widget-signature?hotel=' + key);
    });
}
