import { test, expect } from '@playwright/test';
async function mockPms(page) {
    await page.route('**/api/**', async (route) => {
        const url = new URL(route.request().url());
        let body = { items: [], total: 0 };
        if (url.pathname === '/api/crm/pms/daily')
            body = {
                date: '2026-10-28', stays: [], services: [], upcomingTransfers: [], totalRooms: 24, occupiedNow: 0, emptyNow: 24, plannedOccupied: 0, arrivals: 0, departures: 0
            };
        else if (url.pathname === '/api/auth/me')
            body = {
                username: 'lagos', hotelId: 'hotel-lagos', hotelKey: 'lagos-beach', hotelName: 'Lagos Beach Hotel'
            };
        else if (url.pathname === '/api/crm/pms/board')
            body = {
                rooms: Array.from({ length: 24 }, (_, index) => ({
                    id: 'room-' + index, roomTypeId: index < 20 ? 'standard' : 'suite',
                    roomTypeName: index < 20 ? 'Standart' : 'Aile Suiti', number: String(101 + index),
                    housekeeping: 'CLEAN', occupantId: null, occupantCode: null, upcomingAssignments: 0
                })), assignments: [], history: []
            };
        else if (url.pathname === '/api/crm/pms/history') {
            const current = Number(url.searchParams.get('page') || 0);
            body = {
                items: [{
                        id: 'audit-' + current, roomId: 'room-0', roomNumber: '101', action: 'ROOM_ASSIGNED',
                        actor: 'personel-sayfa-' + (current + 1), createdAt: '2026-10-28T10:00:00Z',
                        beforeState: '{"room":"Atanmamış"}', afterState: '{"room":"101"}'
                    }], page: current, size: 20, total: 45
            };
        }
        else if (url.pathname.endsWith('/services'))
            body = [
                ['BREAKFAST', 'Sabah kahvaltısı'], ['LUNCH', 'Öğle yemeği'], ['DINNER', 'Akşam yemeği'],
                ['VALET', 'Vale ve otopark'], ['TRANSFER', 'Havalimanı transferi']
            ].map(([code, name]) => ({
                code, name, description: 'Hizmet paketi', included: code === 'BREAKFAST', price: 300, unit: 'PER_STAY'
            }));
        else if (url.pathname.endsWith('/rooms'))
            body = [{ id: 'standard', name: 'Standart' }, { id: 'suite', name: 'Aile Suiti' }];
        await route.fulfill({
            status: 200, json: body, headers: { 'X-CSRF-TOKEN': 'test-csrf' }
        });
    });
    await page.goto('/crm/pms');
    await expect(page.getByRole('button', { name: 'Odalar ve temizlik', exact: true })).toBeVisible();
}
test('suite filter finds rooms beyond the first page and combines with room search', async ({ page }) => {
    await mockPms(page);
    await page.getByRole('button', { name: 'Odalar ve temizlik', exact: true }).click();
    const board = page.locator('pms-room-board');
    await expect(board.locator('article')).toHaveCount(12);
    await board.getByLabel('Oda tipi', { exact: true }).selectOption('suite');
    await expect(board.locator('article')).toHaveCount(4);
    await expect(board.locator('article').first()).toContainText('Aile Suiti');
    await board.getByRole('searchbox').fill('121');
    await expect(board.locator('article')).toHaveCount(1);
    await expect(board.locator('article')).toContainText('121');
});
test('history loads server pages, disables boundaries and permits returning', async ({ page }) => {
    await mockPms(page);
    await page.getByRole('button', { name: 'İşlem geçmişi', exact: true }).click();
    await expect(page.locator('pms-audit-timeline')).toContainText('personel-sayfa-1');
    await expect(page.getByRole('button', { name: 'Önceki sayfa', exact: true })).toBeDisabled();
    await page.getByRole('button', { name: 'Sonraki sayfa', exact: true }).click();
    await expect(page.locator('pms-audit-timeline')).toContainText('personel-sayfa-2');
    await page.getByRole('button', { name: 'Sonraki sayfa', exact: true }).click();
    await expect(page.locator('pms-audit-timeline')).toContainText('personel-sayfa-3');
    await expect(page.getByRole('button', { name: 'Sonraki sayfa', exact: true })).toBeDisabled();
    await page.getByRole('button', { name: 'Önceki sayfa', exact: true }).click();
    await expect(page.locator('pms-audit-timeline')).toContainText('personel-sayfa-2');
});
test('service tab displays all meals, valet and transfer', async ({ page }) => {
    await mockPms(page);
    await page.getByRole('button', { name: 'Yemek, vale ve transfer', exact: true }).click();
    const catalog = page.locator('pms-service-catalog');
    for (const name of [
        'Sabah kahvaltısı', 'Öğle yemeği', 'Akşam yemeği', 'Vale ve otopark', 'Havalimanı transferi'
    ]) {
        await expect(catalog.getByRole('heading', { name, exact: true })).toBeVisible();
    }
    await expect(page.getByRole('heading', { name: 'Hizmet takibi için rezervasyon seçin.' })).toBeVisible();
});
