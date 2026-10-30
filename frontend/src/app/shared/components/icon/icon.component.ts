import { Component, input } from '@angular/core';
const paths: Record<string, string> = {
    menu: 'M4 6h16 M4 12h16 M4 18h16',
    users: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M20 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75',
    calendar: 'M8 2v4 M16 2v4 M3 10h18 M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2',
    down: 'M12 4v16 M5 13l7 7 7-7',
    chart: 'M3 3v18h18 M8 16v-5 M13 16V7 M18 16v-8',
    logout: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9',
    refresh: 'M20 7v5h-5 M4 17v-5h5 M6.1 7a7 7 0 0 1 11.6-2L20 8 M4 16l2.3 3A7 7 0 0 0 18 17',
    search: 'M21 21l-4.35-4.35 M19 11a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
    filter: 'M4 7h16 M7 12h10 M10 17h4',
    chevron: 'M9 5l7 7-7 7',
    back: 'M15 5l-7 7 7 7',
    sort: 'M8 3v18 M4 7l4-4 4 4 M16 21V3 M12 17l4 4 4-4',
    mail: 'M3 5h18v14H3z M3 5l9 8 9-8',
    phone: 'M22 16.9V20a2 2 0 0 1-2.2 2A19.8 19.8 0 0 1 3 5.2 2 2 0 0 1 5 3h3l2 5-2 2a15 15 0 0 0 6 6l2-2z',
    trash: 'M3 6h18 M9 6V3h6v3 M5 6l1 15h12l1-15 M10 10v7 M14 10v7',
    note: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z M14 2v6h6 M8 13h8 M8 17h5',
    tag: 'M20 13l-7 7L3 10V3h7z M7 7h.01',
    shield: 'M12 2l9 4v6c0 5-9 10-9 10S3 17 3 12V6z M8 12l3 3 5-6',
    check: 'M5 12l4 4L19 6',
    close: 'M6 6l12 12 M6 18L18 6',
    eye: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12 M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
    bed: 'M3 18V6 M3 14h18v7 M3 18h18 M7 10h4v4H7z M11 10h7a3 3 0 0 1 3 3v1',
    hotel: 'M3 21h18 M5 21V3h14v18 M9 21v-4h6v4 M9 7h.01 M15 7h.01 M9 11h.01 M15 11h.01',
    plus: 'M12 5v14 M5 12h14',
    clock: 'M12 8v5l3 2 M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0'
};
@Component({
    selector: 'ui-icon', standalone: true,
    template: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path [attr.d]="paths[name()] || paths['chevron']" /></svg>`,
    styles: [':host { display: inline-flex; width: 18px; height: 18px; flex: 0 0 auto; align-items: center; justify-content: center; } svg { width: 100%; height: 100%; }']
})
export class IconComponent {
    readonly name = input('chevron');
    readonly paths = paths;
}
