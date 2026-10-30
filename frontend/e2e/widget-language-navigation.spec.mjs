import { test, expect } from '@playwright/test';
import { mockBooking } from './fixtures/showcase-api.mjs';
for (const key of ['lagos-beach', 'peri-stone']) {
    for (const variant of [
        'widget', 'widget-premium', 'widget-signature', 'widget-atelier', 'widget-maison', 'widget-quiet'
    ]) {
        test(`${variant}/${key}: room and service content follows EN/TR`, async ({ page }) => {
            await mockBooking(page);
            await page.goto('/' + variant + '?hotel=' + key);
            await page.getByRole('button', { name: 'Change language', exact: true }).click();
            await page.getByRole('button', { name: 'Find a room', exact: true }).click();
            const englishName = key === 'peri-stone' ? 'Cave Room' : 'Standard Room';
            const turkishName = key === 'peri-stone' ? 'Mağara Oda' : 'Standart';
            await expect(page.getByRole('heading', { name: englishName, exact: true })).toBeVisible();
            if (variant !== 'widget') {
                await page.locator('.room-photo').first().click();
                const dialog = page.getByRole('dialog');
                await expect(dialog.getByRole('heading', { name: englishName, exact: true })).toBeVisible();
                await expect(dialog).toHaveClass(/neutral-gallery/);
                await expect(dialog).not.toHaveClass(/cave-gallery/);
                await dialog.getByRole('button', { name: 'Close room details', exact: true }).click();
            }
            await page.getByRole('button', { name: 'Choose room', exact: true }).first().click();
            await expect(page.locator('.booking-form h2')).toHaveText(englishName);
            for (const name of [
                'Breakfast', 'Lunch', 'Dinner', 'Valet and parking', 'Airport transfer', 'Wi-Fi'
            ]) {
                await expect(page.getByRole('checkbox', { name, exact: true })).toBeVisible();
            }
            await expect(page.getByText('Charged per night for one vehicle.', { exact: true })).toBeVisible();
            await page.getByRole('button', { name: 'Change language', exact: true }).click();
            await expect(page.locator('.booking-form h2')).toHaveText(turkishName);
            for (const name of [
                'Sabah kahvaltısı', 'Öğle yemeği', 'Akşam yemeği', 'Vale ve otopark', 'Havalimanı transferi'
            ]) {
                await expect(page.getByRole('checkbox', { name, exact: true })).toBeVisible();
            }
        });
    }
}
for (const route of [
    'signature/lagos-beach', 'signature/peri-stone', 'showcase/lagos-beach', 'showcase/peri-stone', 'atelier/lagos-beach', 'atelier/peri-stone', 'maison/lagos-beach', 'maison/peri-stone', 'quiet/lagos-beach', 'quiet/peri-stone'
]) {
    test(`${route}: booking links align the widget rather than the introductory heading`, async ({ page }) => {
        await page.setViewportSize({ width: 1440, height: 1000 });
        await mockBooking(page);
        await page.goto('/' + route + '?source=demo');
        const iframe = page.locator('showcase-booking-embed#booking > iframe');
        const widget = page.frameLocator('showcase-booking-embed#booking > iframe');
        await expect(widget.getByLabel('Giriş tarihi', { exact: true })).toHaveValue(/^\d{4}-\d{2}-\d{2}$/);
        const link = page.locator('header a.nav-book, header a.header-book, header a.book-link, header a.atelier-header-book, header a.collection-book');
        for (let repeat = 0; repeat < 2; repeat++) {
            await link.click();
            await expect(page).toHaveURL(new RegExp('/' + route + '\\?source=demo#booking$'));
            await expect.poll(async () => {
                const box = await iframe.boundingBox();
                const header = await page.locator('.signature-header, .site-header, .peri-header, .atelier-header, .collection-header').boundingBox();
                if (!box || !header)
                    return Infinity;
                const target = header.height + Math.max(24, (1000 - header.height - box.height) / 2);
                return Math.abs(box.y - target);
            }).toBeLessThan(25);
        }
    });
}
