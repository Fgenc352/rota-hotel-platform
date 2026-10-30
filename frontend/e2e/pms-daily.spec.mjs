import { test, expect } from '@playwright/test';
function day(offset = 0) {
    const date = new Date();
    date.setDate(date.getDate() + offset);
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}
async function setup(page) {
    const saved = new Map();
    const room = {
        id: 'room-1', roomTypeId: 'standard', roomTypeName: 'Standart', number: '101', housekeeping: 'CLEAN', occupantId: 'stay-1', occupantCode: 'RT-DAILY', upcomingAssignments: 0
    };
    const task = (code, date) => saved.get(code + date) || {
        reservationId: 'stay-1', code, date, status: 'PENDING', time: null, pickup: '', destination: '', plate: '', notes: '', passengers: 2, actor: null, updatedAt: null
    };
    const item = (code, date) => ({
        reservationId: 'stay-1', bookingCode: 'RT-DAILY', guestName: 'Deniz Test', phone: '+905551234567', roomNumber: '101', reservationStatus: 'CHECKED_IN', serviceName: code === 'BREAKFAST' ? 'Sabah kahvaltısı' : 'Havalimanı transferi', quantity: 2, task: task(code, date)
    });
    await page.route('**/api/**', async (route) => {
        const request = route.request();
        const url = new URL(request.url());
        let body = { items: [], total: 0 };
        if (request.method() === 'POST' && url.pathname.includes('/daily-services/')) {
            expect(request.headers()['x-csrf-token']).toBe('test-csrf');
            const value = request.postDataJSON();
            expect(Object.keys(value).sort()).toEqual([
                'date', 'status', 'time', 'pickup', 'destination', 'plate', 'notes', 'passengers'
            ].sort());
            const code = url.pathname.split('/').at(-1);
            saved.set(code + value.date, {
                ...value, reservationId: 'stay-1', code, actor: 'staff', updatedAt: new Date().toISOString()
            });
            await route.fulfill({ status: 204 });
            return;
        }
        if (url.pathname === '/api/auth/me')
            body = {
                username: 'lagos', hotelId: 'hotel-lagos', hotelKey: 'lagos-beach', hotelName: 'Lagos Beach Hotel'
            };
        else if (url.pathname.endsWith('/history') && url.pathname.includes('/daily-services/'))
            body = [];
        else if (url.pathname === '/api/crm/pms/board')
            body = {
                rooms: [room, {
                        ...room, id: 'room-2', number: '102', occupantId: null, occupantCode: null
                    }], assignments: [], history: []
            };
        else if (url.pathname === '/api/crm/pms/history')
            body = {
                items: [], total: 0, page: 0, size: 20
            };
        else if (url.pathname === '/api/crm/pms/daily') {
            const date = url.searchParams.get('date') || day();
            body = {
                date, stays: [{
                        reservationId: 'stay-1', code: 'RT-DAILY', guestName: 'Deniz Test', phone: '+905551234567', roomNumber: '101', roomType: 'Standart', people: 2, checkIn: day(-1), checkOut: day(2), status: 'CHECKED_IN', arrival: false, departure: false
                    }],
                services: [item('BREAKFAST', date)], upcomingTransfers: [item('TRANSFER', day(1))], totalRooms: 2, occupiedNow: 1, emptyNow: 1, plannedOccupied: 1, arrivals: 0, departures: 0
            };
        }
        else if (url.pathname.endsWith('/rooms') || url.pathname.endsWith('/services'))
            body = [];
        await route.fulfill({
            status: 200, json: body, headers: { 'X-CSRF-TOKEN': 'test-csrf' }
        });
    });
    await page.goto('/crm/pms');
    await expect(page.getByRole('heading', { name: 'Bugünün her ayrıntısı.' })).toBeVisible();
    return saved;
}
test('daily meal completion applies only to the selected date', async ({ page }) => {
    await setup(page);
    const meal = page.locator('.daily-service-card').filter({ hasText: 'Sabah kahvaltısı' });
    await meal.getByRole('button', { name: 'Günlük işlem / geçmiş' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel('İşlem durumu').selectOption('SERVED');
    await dialog.getByRole('button', { name: 'Günlük işlemi kaydet' }).click();
    await expect(dialog).toHaveCount(0);
    await expect(meal).toContainText('Servis edildi');
    await page.getByLabel('Operasyon tarihi').fill(day(1));
    await expect(meal).toContainText('Bekliyor');
});
test('upcoming transfer can be scheduled with time, route and passenger count', async ({ page }) => {
    const saved = await setup(page);
    await page.getByRole('button', { name: 'Transferi planla' }).click();
    const dialog = page.getByRole('dialog');
    await dialog.getByLabel('İşlem durumu').selectOption('PLANNED');
    await dialog.getByLabel('Saat', { exact: true }).fill('09:30');
    await dialog.getByLabel('Alış noktası').fill('Antalya Havalimanı');
    await dialog.getByLabel('Varış noktası').fill('Lagos Beach Hotel');
    await dialog.getByLabel('Yolcu sayısı').fill('2');
    await dialog.getByRole('button', { name: 'Günlük işlemi kaydet' }).click();
    await expect(dialog).toHaveCount(0);
    expect(saved.get('TRANSFER' + day(1))).toMatchObject({
        status: 'PLANNED', pickup: 'Antalya Havalimanı', destination: 'Lagos Beach Hotel', passengers: 2
    });
    await expect(page.getByRole('cell', { name: 'Planlandı', exact: true })).toBeVisible();
});
test('current room overview separates occupied and empty physical rooms', async ({ page }) => {
    await setup(page);
    const rooms = page.locator('pms-daily-room-summary');
    await rooms.getByLabel('Oda doluluğu').selectOption('EMPTY');
    await expect(rooms.locator('article')).toHaveCount(1);
    await expect(rooms.locator('article')).toContainText('102');
    await rooms.getByLabel('Oda doluluğu').selectOption('OCCUPIED');
    await expect(rooms.locator('article')).toHaveCount(1);
    await expect(rooms.locator('article')).toContainText('101');
    await expect(rooms.locator('article')).toContainText('RT-DAILY');
});
