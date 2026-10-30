import { test, expect } from '@playwright/test';
import { mockBooking, roomFor } from './fixtures/showcase-api.mjs';
for (const edition of ['maison', 'quiet']) {
    for (const key of ['lagos-beach', 'peri-stone']) {
        for (const mobile of [false, true]) {
            test(`${edition}/${key}: hotel, gallery and room preference (${mobile ? 'mobile' : 'desktop'})`, async ({ page }) => {
                await mockBooking(page);
                await page.setViewportSize(mobile ? { width: 390, height: 844 } : { width: 1440, height: 1000 });
                await page.goto('/' + edition + '/' + key);
                await expect(page.locator('.' + edition + '-estate')).toBeVisible();
                await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
                await expect(page.locator('showcase-booking-embed > iframe')).toHaveAttribute('src', '/widget-' + edition + '?hotel=' + key);
                if (mobile) {
                    await page.getByRole('button', { name: 'Menüyü aç veya kapat' }).click();
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
                await expect(page.locator('showcase-booking-embed > iframe')).toHaveAttribute('src', '/widget-' + edition + '?hotel=' + key + '&room=' + roomFor(key).id);
                await expect(page.frameLocator('showcase-booking-embed > iframe').locator('.' + edition + '-widget')).toBeVisible();
                expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
            });
        }
        test(`${edition}/${key}: language, retained inputs, services and confirmed booking`, async ({ page }) => {
            const { bookings } = await mockBooking(page);
            await page.goto('/widget-' + edition + '?hotel=' + key);
            await page.getByRole('button', { name: 'Oda ara', exact: true }).click();
            await expect(page.getByRole('region', { name: 'Konaklama fiyat hesabı' })).toBeVisible();
            await page.getByRole('button', { name: 'Odayı seç', exact: true }).click();
            await page.getByLabel('Ad', { exact: true }).fill('Deniz');
            await page.getByLabel('Soyad', { exact: true }).fill('Collection');
            await page.getByLabel('E-posta', { exact: true }).fill('collection@example.com');
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
    }
}
for (const edition of ['maison', 'quiet']) {
    for (const key of ['lagos-beach', 'peri-stone']) {
        test(`${edition}/${key}: public links stay on the hotel and never enter CRM`, async ({ page }) => {
            await mockBooking(page);
            const path = '/' + edition + '/' + key;
            await page.goto(path);
            if (edition === 'maison') {
                const discover = page.getByRole('link', { name: 'Otelin hikâyesini keşfet', exact: true });
                await expect(discover).toHaveAttribute('href', path + '#story');
                await discover.click();
                await expect(page).toHaveURL(path + '#story');
            }
            const rooms = page.locator(edition === 'maison' ? '.invitation-copy .estate-link' : '.quiet-story-copy .estate-link');
            await expect(rooms).toHaveAttribute('href', path + '#residences');
            await rooms.click();
            await expect(page).toHaveURL(path + '#residences');
            await expect(page.locator('collection-footer a[href="/login"], collection-footer a[href^="/crm"]')).toHaveCount(0);
            await page.locator('collection-footer .footer-baseline a').click();
            await expect(page).toHaveURL(path + '#booking');
            await expect(page.locator('.collection-estate')).toBeVisible();
        });
    }
}
