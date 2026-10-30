export function roomFor(key) {
    const cave = key === 'peri-stone';
    return {
        id: cave ? '20000000-0000-0000-0000-000000000004' : '20000000-0000-0000-0000-000000000001',
        name: cave ? 'Mağara Oda' : 'Standart', description: 'Doğal dokularla tasarlanmış, dinlenmeye ayrılmış bir alan.',
        capacity: 2, areaSquareMeters: cave ? 32 : 28, bedDescription: '1 çift kişilik yatak',
        nightlyPrice: cave ? 5500 : 3200, currency: 'TRY', minNights: 1,
        amenities: ['Wi-Fi', 'Özel banyo', 'Kahve köşesi'],
        images: [`/images/showcase/${cave ? 'peri' : 'lagos'}-room.webp`, ...[
                'reverse', 'bathroom', 'living', 'evening'
            ].map(view => `/images/showcase/${cave ? 'peri-cave' : 'lagos-standard'}-${view}.webp`)]
    };
}
export async function mockBooking(page, options = {}) {
    const bookings = [];
    const publicRequests = [];
    await page.route('**/api/**', async (route) => {
        const url = new URL(route.request().url());
        const key = url.pathname.includes('peri-stone') ? 'peri-stone' : 'lagos-beach';
        const room = roomFor(key);
        publicRequests.push(url.pathname);
        let body;
        if (url.pathname.endsWith('/services'))
            body = [
                {
                    code: 'BREAKFAST', name: 'Sabah kahvaltısı', description: 'Kahvaltı dahil.', price: 0, unit: 'PER_PERSON_PER_NIGHT', included: true
                },
                {
                    code: 'VALET', name: 'Vale ve otopark', description: 'Bir araç için.', price: 300, unit: 'PER_NIGHT', included: false
                },
                {
                    code: 'LUNCH', name: 'Öğle yemeği', description: 'Kişi ve gece başına.', price: 650, unit: 'PER_PERSON_PER_NIGHT', included: false
                },
                {
                    code: 'DINNER', name: 'Akşam yemeği', description: 'Kişi ve gece başına.', price: 950, unit: 'PER_PERSON_PER_NIGHT', included: false
                },
                {
                    code: 'TRANSFER', name: 'Havalimanı transferi', description: 'Tek yön.', price: 1800, unit: 'PER_STAY', included: false
                },
                {
                    code: 'WIFI', name: 'Wi-Fi', description: 'İnternet dahil.', price: 0, unit: 'PER_STAY', included: true
                }
            ];
        else if (url.pathname.endsWith('/rooms'))
            body = [room];
        else if (url.pathname.endsWith('/availability')) {
            const nights = [];
            const cursor = new Date(url.searchParams.get('checkIn') + 'T12:00:00Z');
            while (cursor.toISOString().slice(0, 10) < url.searchParams.get('checkOut')) {
                nights.push({ date: cursor.toISOString().slice(0, 10), price: room.nightlyPrice });
                cursor.setUTCDate(cursor.getUTCDate() + 1);
            }
            body = options.empty ? [] : [{
                    roomTypeId: room.id, name: room.name, capacity: 2, remaining: 3,
                    nightlyPrice: room.nightlyPrice, total: room.nightlyPrice * nights.length, currency: 'TRY', minNights: 1, nights
                }];
        }
        else if (url.pathname.endsWith('/reservations')) {
            const request = route.request().postDataJSON();
            bookings.push({
                path: url.pathname, body: request, headers: route.request().headers()
            });
            const nights = (Date.parse(request.checkOut + 'T12:00:00Z') - Date.parse(request.checkIn + 'T12:00:00Z')) / 86400000;
            body = {
                id: 'reservation', code: 'RT-SHOWCASE-01', total: room.nightlyPrice * nights + (request.serviceCodes?.includes('VALET') ? 300 * nights : 0), status: 'CONFIRMED', currency: 'TRY'
            };
        }
        else if (url.pathname.endsWith('/funnel')) {
            await route.fulfill({ status: 200, body: '' });
            return;
        }
        else {
            body = {
                key, name: key === 'peri-stone' ? 'Peri Stone Cave Hotel' : 'Lagos Beach Hotel',
                location: key === 'peri-stone' ? 'Kapadokya' : 'Antalya', timezone: 'Europe/Istanbul', eurRate: 40, privacyVersion: 'demo-v1'
            };
        }
        await route.fulfill({ status: 200, json: body });
    });
    return { bookings, publicRequests };
}
