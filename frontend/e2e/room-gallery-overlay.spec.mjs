import { test, expect } from '@playwright/test';
import { mockBooking, roomFor } from './fixtures/showcase-api.mjs';
for (const key of ['lagos-beach', 'peri-stone']) {
    for (const mobile of [false, true]) {
        test(`${key}: gallery is centered in the host viewport and closing preserves the form (${mobile ? 'mobile' : 'desktop'})`, async ({ page }) => {
            const viewport = mobile ? { width: 390, height: 844 } : { width: 1440, height: 1000 };
            await page.setViewportSize(viewport);
            await mockBooking(page);
            await page.goto('/signature/' + key);
            const booking = page.frameLocator('showcase-booking-embed > iframe');
            await page.locator('showcase-booking-embed > iframe').scrollIntoViewIfNeeded();
            await booking.getByRole('button', { name: 'Oda ara', exact: true }).click();
            await booking.getByRole('button', { name: /Odayı seç/ }).first().click();
            await booking.getByLabel('Ad', { exact: true }).fill('Korunan');
            await booking.getByRole('button', { name: /fotoğrafı keşfet/ }).click();
            const host = page.locator('showcase-hosted-room-gallery');
            await expect(host.getByRole('dialog')).toBeVisible();
            const gallery = page.frameLocator('showcase-hosted-room-gallery iframe');
            const dialog = gallery.getByRole('dialog');
            await expect(dialog).toBeVisible();
            await expect(dialog).toHaveClass(/neutral-gallery/);
            await expect(dialog).not.toHaveClass(/cave-gallery/);
            await expect.poll(async () => {
                const box = await dialog.boundingBox();
                return box ? Math.abs(box.y + box.height / 2 - viewport.height / 2) : Infinity;
            }).toBeLessThan(4);
            await expect(dialog.locator('.large-image')).toHaveCSS('object-fit', 'contain');
            await expect(dialog.locator('.thumbnails button')).toHaveCount(5);
            await dialog.getByRole('button', { name: 'Fotoğraf 5', exact: true }).click();
            await expect(dialog.locator('.large-image')).toHaveAttribute('src', roomFor(key).images[4]);
            await dialog.getByRole('button', { name: 'Oda detayını kapat', exact: true }).click();
            await expect(host).toHaveCount(0);
            await expect(booking.getByLabel('Ad', { exact: true })).toHaveValue('Korunan');
            expect(await page.evaluate(() => document.documentElement.style.overflow)).not.toBe('hidden');
        });
    }
}
