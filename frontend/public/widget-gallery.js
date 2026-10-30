(() => {
    'use strict';
    const hostKey = Symbol.for('rota.roomGalleryHost');
    if (window[hostKey])
        return;
    let closeActive;
    window[hostKey] = ({ origin, hotelKey, frame, request }) => {
        if (closeActive)
            closeActive(false);
        const scrollY = window.scrollY;
        const root = document.documentElement;
        const previousOverflow = root.style.overflow;
        const previousGutter = root.style.scrollbarGutter;
        const dialog = document.createElement('dialog');
        dialog.className = 'rota-room-gallery-host';
        dialog.setAttribute('aria-label', request.language === 'en' ? 'Room photos and details' : 'Oda fotoğrafları ve detayları');
        dialog.style.cssText = 'position:fixed!important;inset:0!important;width:100vw!important;height:100dvh!important;max-width:none!important;max-height:none!important;margin:0!important;padding:0!important;border:0!important;background:transparent!important;overflow:hidden!important;';
        const style = document.createElement('style');
        style.textContent = '.rota-room-gallery-host::backdrop{background:rgba(110,110,110,.28)!important;backdrop-filter:blur(12px)!important;}';
        const viewer = document.createElement('iframe');
        const query = new URLSearchParams({
            hotel: hotelKey, room: request.roomId,
            currency: request.currency, lang: request.language, selectable: String(request.selectable)
        });
        viewer.src = new URL('/room-gallery?' + query, origin).href;
        viewer.title = request.language === 'en' ? 'Room photos and details' : 'Oda fotoğrafları ve detayları';
        viewer.setAttribute('sandbox', 'allow-scripts allow-same-origin');
        viewer.style.cssText = 'display:block!important;width:100%!important;height:100%!important;max-width:none!important;border:0!important;background:transparent!important;color-scheme:light!important;';
        dialog.appendChild(viewer);
        document.head.appendChild(style);
        document.body.appendChild(dialog);
        root.style.scrollbarGutter = 'stable';
        root.style.overflow = 'hidden';
        let finished = false;
        function finish(selected) {
            if (finished)
                return;
            finished = true;
            window.removeEventListener('message', message);
            dialog.close();
            dialog.remove();
            style.remove();
            root.style.overflow = previousOverflow;
            root.style.scrollbarGutter = previousGutter;
            window.scrollTo({ top: scrollY, behavior: 'instant' });
            frame.contentWindow.postMessage({
                type: 'rota:gallery:result', roomId: request.roomId, selected
            }, origin);
            closeActive = undefined;
        }
        function message(event) {
            if (event.origin !== origin || event.source !== viewer.contentWindow || event.data?.roomId !== request.roomId)
                return;
            if (event.data.type === 'rota:gallery:close')
                finish(false);
            if (event.data.type === 'rota:gallery:select' && request.selectable)
                finish(true);
        }
        dialog.addEventListener('cancel', event => {
            event.preventDefault();
            finish(false);
        });
        window.addEventListener('message', message);
        closeActive = finish;
        dialog.showModal();
        frame.contentWindow.postMessage({ type: 'rota:gallery:hosted', roomId: request.roomId }, origin);
    };
})();
