import { test, expect } from '@playwright/test';
import { roomFor, mockBooking } from './fixtures/showcase-api.mjs';
for (const route of ['widget-premium', 'widget-signature', 'widget-atelier']) {
    test(`${route}: room and date steps are clickable, guest details survive, edited dates invalidate old quotes`, async ({ page }) => {
        const { publicRequests, bookings } = await mockBooking(page);
        await page.goto('/' + route + '?hotel=lagos-beach');
        await expect(page.getByRole('button', { name: /Odanız/ })).toBeDisabled();
        await page.getByRole('button', { name: 'Oda ara', exact: true }).click();
        await page.getByRole('button', { name: /Odayı seç/ }).click();
        await page.getByLabel('Ad', { exact: true }).fill('Deniz');
        await page.getByLabel('Soyad', { exact: true }).fill('Korunan');
        await page.getByLabel('E-posta', { exact: true }).fill('saved@example.com');
        const requestCount = publicRequests.filter(url => url.endsWith('/availability')).length;
        await page.getByRole('button', { name: /Odanız/ }).click();
        await expect(page.getByRole('button', { name: /Odayı seç/ })).toBeVisible();
        expect(publicRequests.filter(url => url.endsWith('/availability'))).toHaveLength(requestCount);
        await page.getByRole('button', { name: /Bilgileriniz/ }).click();
        await expect(page.getByLabel('Ad', { exact: true })).toHaveValue('Deniz');
        await expect(page.getByLabel('Soyad', { exact: true })).toHaveValue('Korunan');
        await expect(page.getByLabel('E-posta', { exact: true })).toHaveValue('saved@example.com');
        await page.getByRole('button', { name: /Tarihleriniz/ }).click();
        await page.getByLabel('Çıkış tarihi', { exact: true }).fill('2027-08-20');
        await expect(page.getByRole('button', { name: /Odanız/ })).toBeDisabled();
        await expect(page.getByRole('button', { name: /Bilgileriniz/ })).toBeDisabled();
        expect(bookings).toEqual([]);
    });
}
for (const key of ['lagos-beach', 'peri-stone']) {
    test(`${key}: signature page has room-specific five-photo galleries, working tabs and its new widget`, async ({ page }) => {
        await mockBooking(page);
        await page.goto('/signature/' + key);
        await expect(page.locator('.signature-hero')).toBeVisible();
        const tabs = page.getByRole('tablist', { name: 'Oda tipleri' });
        await expect(tabs).toBeVisible();
        await expect(page.locator('.room-thumbnails button')).toHaveCount(5);
        await page.getByRole('button', { name: 'Sonraki oda fotoğrafı', exact: true }).click();
        await expect(page.locator('.gallery-trigger img')).toHaveAttribute('src', roomFor(key).images[1]);
        await page.locator('.gallery-trigger').click();
        const dialog = page.getByRole('dialog');
        await expect(dialog).toBeVisible();
        await expect(dialog.locator('.thumbnails button')).toHaveCount(5);
        await dialog.getByRole('button', { name: 'Fotoğraf 5', exact: true }).click();
        await expect(dialog.locator('.large-image')).toHaveAttribute('src', roomFor(key).images[4]);
        await page.keyboard.press('ArrowRight');
        await expect(dialog.locator('.large-image')).toHaveAttribute('src', roomFor(key).images[0]);
        await page.keyboard.press('Escape');
        await page.getByRole('button', { name: /Bu odayı seç/ }).click();
        await expect(page.locator('iframe')).toHaveAttribute('src', /\/widget-signature\?hotel=/);
        await expect(page.frameLocator('iframe').locator('.signature-preferred')).toContainText(roomFor(key).name);
    });
}
test('signature widget preserves booking contract, stops navigation after confirmation and resets explicitly', async ({ page }) => {
    const { bookings } = await mockBooking(page);
    await page.goto('/widget-signature?hotel=peri-stone');
    await page.getByRole('button', { name: 'Oda ara', exact: true }).click();
    await page.getByRole('button', { name: /Odayı seç/ }).click();
    await page.getByLabel('Ad', { exact: true }).fill('Deniz');
    await page.getByLabel('Soyad', { exact: true }).fill('Test');
    await page.getByLabel('E-posta', { exact: true }).fill('signature@example.com');
    await page.getByLabel('Telefon (ülke koduyla)', { exact: true }).fill('+905550000099');
    await page.getByLabel('KVKK aydınlatma metnini okudum.').check();
    await page.getByRole('button', { name: 'Rezervasyonu tamamla', exact: true }).click();
    await expect(page.locator('.booking-code')).toHaveText('RT-SHOWCASE-01');
    expect(bookings).toHaveLength(1);
    expect(bookings[0].path).toBe('/api/public/hotels/peri-stone/reservations');
    expect(bookings[0].headers['idempotency-key']).toBeTruthy();
    await expect(page.getByRole('button', { name: /Tarihleriniz/ })).toBeDisabled();
    await expect(page.getByRole('button', { name: /Odanız/ })).toBeDisabled();
    await page.getByRole('button', { name: 'Yeni rezervasyon', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Oda ara', exact: true })).toBeVisible();
});
test('signature pages and widget stay usable on mobile with reduced motion', async ({ page }) => {
    await mockBooking(page);
    await page.setViewportSize({ width: 390, height: 844 });
    await page.emulateMedia({ reducedMotion: 'reduce' });
    for (const path of ['/signature/lagos-beach', '/signature/peri-stone', '/widget-signature?hotel=lagos-beach']) {
        await page.goto(path);
        await expect(page.locator('h1').first()).toBeVisible();
        expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    }
});
for (const route of [
    'signature/lagos-beach', 'signature/peri-stone', 'showcase/lagos-beach', 'showcase/peri-stone'
]) {
    test(`${route}: section links stay on the public hotel page`, async ({ page }) => {
        await mockBooking(page);
        await page.goto('/' + route + '?source=demo');
        const discover = page.locator(route.startsWith('signature/')
            ? '.signature-hero a[href$="#story"]'
            : route.endsWith('lagos-beach') ? '.hero-discover' : '.discover');
        const expectedFragment = route.startsWith('signature/') ? 'story' : 'rooms';
        await discover.click();
        await expect(page).toHaveURL(new RegExp('/' + route + '\\?source=demo#' + expectedFragment + '$'));
        await expect(page.locator('#' + expectedFragment)).toBeInViewport();
    });
}
for (const widget of [
    'widget', 'widget-premium', 'widget-signature', 'widget-atelier'
]) {
    test(`${widget}: included services and paid extras appear before confirmation`, async ({ page }) => {
        const { bookings } = await mockBooking(page);
        await page.goto('/' + widget + '?hotel=lagos-beach');
        const search = widget === 'widget' ? page.getByRole('button', { name: /Müsaitlik|Oda ara|Search/ }).first()
            : page.getByRole('button', { name: 'Oda ara', exact: true });
        await search.click();
        await page.getByRole('button', { name: /Odayı seç|Bu odayı seç|Select room/ }).first().click();
        await expect(page.getByRole('checkbox', { name: /Sabah kahvaltısı/ })).toBeChecked();
        await expect(page.getByRole('checkbox', { name: /Sabah kahvaltısı/ })).toBeDisabled();
        await page.getByRole('checkbox', { name: /Vale ve otopark/ }).check();
        await expect(page.locator('.service-totals')).toContainText('Genel toplam');
        await page.getByLabel('Ad', { exact: true }).fill('Deniz');
        await page.getByLabel('Soyad', { exact: true }).fill('Test');
        await page.getByLabel('E-posta', { exact: true }).fill('extras@example.com');
        await page.getByLabel('Telefon (ülke koduyla)', { exact: true }).fill('+905551234567');
        await page.getByRole('checkbox', { name: /KVKK/ }).check();
        await page.getByRole('button', { name: 'Rezervasyonu tamamla', exact: true }).click();
        await expect.poll(() => bookings.length).toBe(1);
        expect(bookings[0].body.serviceCodes).toEqual(['VALET']);
        expect(bookings[0].body).not.toHaveProperty('total');
    });
}
