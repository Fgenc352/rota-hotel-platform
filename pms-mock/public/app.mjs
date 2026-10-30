const $ = id => document.getElementById(id);
const statusLabel = value => ({
    PENDING: 'Bekliyor', CONFIRMED: 'Onaylandı', CHECKED_IN: 'Giriş yapıldı', CHECKED_OUT: 'Çıkış yapıldı', CANCELLED: 'İptal', NO_SHOW: 'Gelmedi'
})[value] || value;
let detailVersion = 0;
const hotels = new Map([
    ['10000000-0000-0000-0000-000000000001', 'Lagos Beach Hotel'],
    ['10000000-0000-0000-0000-000000000002', 'Peri Stone Cave Hotel']
]);
const state = {
    page: 0, total: 0, items: [], simulation: { paused: false, delayMs: 0 }, loading: false
};
const element = (tag, text, className) => {
    const node = document.createElement(tag);
    if (text !== undefined)
        node.textContent = text;
    if (className)
        node.className = className;
    return node;
};
const date = value => value ? new Intl.DateTimeFormat('tr-TR', {
    day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC'
}).format(new Date(value + 'T00:00:00Z')) : '—';
const instant = value => new Intl.DateTimeFormat('tr-TR', {
    day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: 'Europe/Istanbul'
}).format(new Date(value));
const hotel = value => hotels.get(value) || value || 'Önceki bildirim';
async function request(path, options) {
    const response = await fetch(path, options);
    if (!response.ok)
        throw new Error('PMS ekranı verileri alınamadı. Servisin açık olduğunu kontrol edin.');
    return response.json();
}
function detail(row) {
    $('detail-title').textContent = row.code || 'Rezervasyon bildirimi';
    const fields = [
        ['Otel', hotel(row.hotelId)], ['Rezervasyon kodu', row.code || '—'],
        ['Giriş tarihi', date(row.checkIn)], ['Çıkış tarihi', date(row.checkOut)],
        ['Güncel durum', statusLabel(row.status || 'CONFIRMED')], ['Son senkronizasyon', instant(row.acceptedAt)], ['Rezervasyon kimliği', row.reservationId || '—'],
        ['Bildirim kimliği (idempotency)', row.eventId]
    ];
    $('detail-fields').replaceChildren(...fields.map(([name, value]) => {
        const group = element('div');
        group.append(element('dt', name), element('dd', value));
        return group;
    }));
    $('detail').showModal();
    const version = ++detailVersion;
    $('detail-history').replaceChildren(element('p', 'Geçmiş yükleniyor…', 'help'));
    if (!row.hotelId || !row.reservationId) {
        $('detail-history').replaceChildren(element('p', 'Önceki sürüm kaydında rezervasyon geçmişi bulunmuyor.', 'help'));
        return;
    }
    request('/api/reservations/' + encodeURIComponent(row.reservationId) + '/history?' + new URLSearchParams({ hotel: row.hotelId }))
        .then(data => {
        if (version !== detailVersion)
            return;
        $('detail-history').replaceChildren(...data.items.map(entry => {
            const item = element('article');
            item.append(element('strong', statusLabel(entry.status || 'CONFIRMED')), element('small', `Sürüm ${entry.version || 1} · ${instant(entry.acceptedAt)}`));
            return item;
        }));
    }).catch(() => {
        if (version === detailVersion)
            $('detail-history').replaceChildren(element('p', 'Durum geçmişi yüklenemedi.', 'help'));
    });
}
function renderReservations(data) {
    state.items = data.items;
    state.total = data.total;
    $('count').textContent = data.total + ' kayıt';
    $('empty').hidden = data.items.length !== 0;
    $('page-label').textContent = `Sayfa ${state.page + 1} · ${data.total} kayıt`;
    $('previous').disabled = state.page === 0;
    $('next').disabled = (state.page + 1) * 20 >= data.total;
    $('reservations-body').replaceChildren(...data.items.map(row => {
        const tr = element('tr');
        const reference = element('td');
        reference.append(element('strong', row.code || 'Önceki bildirim'), element('small', row.eventId.slice(0, 8)));
        const status = element('td');
        status.append(element('span', statusLabel(row.status || 'CONFIRMED'), 'badge neutral'), element('small', 'Sürüm ' + (row.version || 1)));
        const action = element('td');
        const button = element('button', 'Detay', 'secondary');
        button.type = 'button';
        button.setAttribute('aria-label', (row.code || row.eventId) + ' detayını aç');
        button.addEventListener('click', () => detail(row));
        action.append(button);
        tr.append(reference, element('td', hotel(row.hotelId)), element('td', date(row.checkIn)), element('td', date(row.checkOut)), element('td', instant(row.acceptedAt)), status, action);
        return tr;
    }));
}
function renderRequests(data) {
    $('requests-body').replaceChildren(...data.items.map(row => {
        const tr = element('tr');
        const result = element('td');
        result.append(element('span', row.status >= 400 ? `${row.status} · Reddedildi` : row.duplicate ? '200 · Tekrar / tek kayıt' : row.stale ? '200 · Eski sürüm / durum korundu' : '200 · Kabul edildi', 'badge' + (row.status >= 400 ? ' error' : row.duplicate ? ' neutral' : '')));
        const reference = element('td');
        reference.append(element('strong', row.code || row.eventId.slice(0, 12)), element('small', hotel(row.hotelId)));
        tr.append(reference, element('td', instant(row.receivedAt)), result);
        return tr;
    }));
    if (!data.items.length) {
        const row = element('tr');
        const cell = element('td', 'Henüz bildirim alınmadı.', 'empty');
        cell.colSpan = 3;
        row.append(cell);
        $('requests-body').append(row);
    }
}
function renderStatus(status) {
    state.simulation = status.simulation;
    const legacy = status.legacyEvents ?? Math.max(0, status.accepted - status.visibleReservations);
    $('legacy-note').hidden = legacy === 0;
    $('legacy-note').textContent = legacy > 0 ? `${legacy} eski bildirim yalnızca olay kimliğiyle saklanmış. Detaylı liste bu sürümle alınan veya tekrar gönderilen bildirimleri gösterir.` : '';
    for (const key of ['accepted', 'failed', 'duplicates'])
        $(key).textContent = String(status[key]);
    $('connection').textContent = status.simulation.paused ? 'Kesinti simülasyonu açık' : 'PMS alımı açık';
    $('connection').classList.toggle('warning', status.simulation.paused);
    $('pause').textContent = status.simulation.paused ? 'Bildirim alımını aç' : 'Kesinti simüle et';
    // Do not overwrite a delay the user is currently editing.
    if (document.activeElement !== $('delay'))
        $('delay').value = String(status.simulation.delayMs);
}
async function refresh() {
    if (state.loading)
        return;
    state.loading = true;
    $('refresh').disabled = true;
    const query = new URLSearchParams({
        page: String(state.page), size: '20', q: $('query').value, hotel: $('hotel').value, status: $('reservation-status').value
    });
    try {
        const [status, reservations, attempts] = await Promise.all([
            request('/api/status'), request('/api/reservations?' + query), request('/api/requests')
        ]);
        renderStatus(status);
        renderReservations(reservations);
        renderRequests(attempts);
        $('error').hidden = true;
    }
    catch (error) {
        $('error').textContent = error.message;
        $('error').hidden = false;
        $('connection').textContent = 'PMS’e ulaşılamıyor';
        $('connection').classList.add('warning');
    }
    finally {
        state.loading = false;
        $('refresh').disabled = false;
    }
}
async function simulate(paused = state.simulation.paused) {
    $('pause').disabled = true;
    try {
        await request('/api/simulation', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ paused, delayMs: Number($('delay').value) })
        });
        await refresh();
    }
    catch (error) {
        $('error').textContent = error.message;
        $('error').hidden = false;
    }
    finally {
        $('pause').disabled = false;
    }
}
$('filters').addEventListener('submit', event => {
    event.preventDefault();
    state.page = 0;
    void refresh();
});
$('refresh').addEventListener('click', refresh);
$('previous').addEventListener('click', () => {
    state.page = Math.max(0, state.page - 1);
    void refresh();
});
$('next').addEventListener('click', () => {
    state.page++;
    void refresh();
});
$('pause').addEventListener('click', () => simulate(!state.simulation.paused));
$('delay').addEventListener('change', () => simulate());
$('close-detail').addEventListener('click', () => $('detail').close());
void refresh();
setInterval(() => {
    if (!document.hidden)
        void refresh();
}, 5000);
