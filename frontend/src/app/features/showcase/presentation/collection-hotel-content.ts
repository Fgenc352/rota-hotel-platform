export const collectionHotels = {
    'lagos-beach': {
        cave: false, name: 'Lagos Beach Hotel', place: 'ANTALYA · AKDENİZ', wordmark: 'lagos',
        hero: '/images/atelier/lagos-atelier-hero.webp', detail: '/images/atelier/lagos-atelier-detail.webp',
        quietHero: '/images/showcase/lagos-hero.webp', room: '/images/showcase/lagos-deluxe-living.webp',
        breakfast: '/images/services/lagos-breakfast.webp', dinner: '/images/services/lagos-dinner.webp',
        eyebrow: 'THE MEDITERRANEAN, AT YOUR OWN PACE',
        story: 'Denizin ritminde, kendi dünyanızda.',
        description: 'Gün ışığıyla uyanan odalar, denize açılan ufuklar ve uzayıp giden sofralar. Lagos’ta güzel bir günün tek bir doğru tarifi yok. Sizin ritminiz var.',
        ritual: 'Denize karşı bir sabah.', evening: 'Akşamın en güzel saati.',
        ritualText: 'Günün ilk ışıkları, taze tatlar ve acele edilmeyen bir kahvaltı.',
        eveningText: 'Sohbetin uzadığı, Akdeniz’in sofranıza eşlik ettiği akşamlar.'
    },
    'peri-stone': {
        cave: true, name: 'Peri Stone Cave Hotel', place: 'KAPADOKYA · ANADOLU', wordmark: 'peri stone',
        hero: '/images/atelier/peri-atelier-hero.webp', detail: '/images/atelier/peri-atelier-detail.webp',
        quietHero: '/images/showcase/peri-hero.webp', room: '/images/showcase/peri-cave-living.webp',
        breakfast: '/images/services/peri-breakfast.webp', dinner: '/images/services/peri-dinner.webp',
        eyebrow: 'CARVED BY TIME. RESERVED FOR YOU.',
        story: 'Taşın hafızasında, size ait bir hikâye.',
        description: 'Yüzyılların şekillendirdiği dokular, kemerlerden süzülen ışık ve vadinin derin sessizliği. Peri Stone, Kapadokya’yı yalnızca seyretmek değil, içinde yaşamak için.',
        ritual: 'Vadinin uyandığı saat.', evening: 'Taş, ışık ve uzun sohbetler.',
        ritualText: 'Yerel tatlar ve taşın sıcaklığıyla yavaş başlayan bir Kapadokya sabahı.',
        eveningText: 'Sıcak ışıkların altında, günü bir sofranın etrafında tamamlayın.'
    }
} as const;
export type CollectionHotelKey = keyof typeof collectionHotels;
