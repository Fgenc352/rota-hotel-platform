(() => {
    'use strict';
    const script = document.currentScript;
    if (!(script instanceof HTMLScriptElement))
        return;
    const key = script.dataset.hotelKey;
    if (!key || !/^[a-z0-9-]{1,80}$/.test(key)) {
        console.error('Rota: valid data-hotel-key is required.');
        return;
    }
    const source = new URL(script.src);
    const frame = document.createElement('iframe');
    frame.src = new URL('/widget-maison?hotel=' + encodeURIComponent(key), source.origin).href;
    frame.title = key === 'peri-stone' ? 'Peri Stone Cave Hotel rezervasyon' : 'Lagos Beach Hotel rezervasyon';
    frame.loading = 'lazy';
    frame.referrerPolicy = 'strict-origin-when-cross-origin';
    frame.setAttribute('sandbox', 'allow-scripts allow-forms allow-same-origin');
    frame.style.cssText = 'display:block!important;width:100%!important;max-width:100%!important;height:760px!important;border:0!important;background:transparent!important;color-scheme:light!important;overflow-anchor:none!important;';
    window.addEventListener('message', event => {
        if (event.origin !== source.origin || event.source !== frame.contentWindow)
            return;
        const data = event.data;
        if (data?.type === 'rota:gallery:open') {
            openGallery(data);
            return;
        }
        if (!Number.isFinite(data?.height) || data.height < 200 || data.height > 10000)
            return;
        if (data.type === 'rota:premium-widget:resize') {
            frame.style.setProperty('height', Math.ceil(data.height) + 4 + 'px', 'important');
        }
        else if (data.type === 'rota:booking:confirmed' && Number.isFinite(data.top)
            && data.top >= 0 && data.top < data.height
            && Number.isFinite(data.cardHeight) && data.cardHeight > 0 && data.top + data.cardHeight <= data.height + 1) {
            frame.style.setProperty('height', Math.ceil(data.height) + 4 + 'px', 'important');
            revealConfirmation(data.top, data.cardHeight);
        }
    });
    function openGallery(request) {
        if (typeof request.roomId !== 'string'
            || !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(request.roomId)
            || !['TRY', 'EUR'].includes(request.currency) || !['tr', 'en'].includes(request.language)
            || typeof request.selectable !== 'boolean')
            return;
        const hostKey = Symbol.for('rota.roomGalleryHost');
        const show = () => window[hostKey]?.({
            origin: source.origin, hotelKey: key, frame, request
        });
        if (window[hostKey]) {
            show();
            return;
        }
        const helper = document.createElement('script');
        helper.src = new URL('/widget-gallery.js', source.origin).href;
        helper.onload = show;
        // If the host blocks this helper, the widget retains its own gallery fallback.
        document.head.appendChild(helper);
    }
    function revealConfirmation(top, cardHeight) {
        // Let the frame shrink before computing the card's position on the host page.
        window.requestAnimationFrame(() => window.requestAnimationFrame(() => {
            const header = document.querySelector?.('.signature-header, .site-header, .peri-header, .atelier-header, .collection-header');
            const headerHeight = header?.getBoundingClientRect().height || 0;
            const offset = headerHeight + Math.max(24, (window.innerHeight - headerHeight - cardHeight) / 2);
            window.scrollTo({
                top: Math.max(0, frame.getBoundingClientRect().top + window.scrollY + top - offset),
                behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth'
            });
        }));
    }
    script.insertAdjacentElement('afterend', frame);
})();
