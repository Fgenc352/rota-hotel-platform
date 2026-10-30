import type { BookingLanguage } from '../models/booking-preferences.model';
const englishText: Readonly<Record<string, string>> = {
    "Standart": "Standard Room",
    "Deluxe Deniz Manzaralı": "Sea View Deluxe",
    "Aile Suiti": "Family Suite",
    "Mağara Oda": "Cave Room",
    "Teras Süit": "Terrace Suite",
    "Yumuşak tonlar, doğal dokular ve dinlenmek için özenle tasarlanmış bir alan. Akdeniz günlerine sakin bir başlangıç.": "Soft tones, natural textures and a thoughtfully designed space to unwind. A peaceful start to your Mediterranean days.",
    "Denizden ilham alan ferah bir konaklama. Geniş yaşam alanı ve gün ışığıyla dolan bir oda.": "An airy retreat inspired by the sea, with generous living space and plenty of natural light.",
    "Birlikte geçirilen anlara daha fazla yer açın. Aile konaklamaları için geniş ve kullanışlı yaşam alanı.": "Make more room for time together, with a spacious and practical living area designed for family stays.",
    "Kapadokya taşının doğal dokusuyla çevrili, kendine özgü bir sığınak. Sıcak ışık, kemerli detaylar ve derin bir sessizlik.": "A distinctive retreat surrounded by natural Cappadocian stone, warm light, arched details and deep tranquility.",
    "Vadinin ışığına açılan özel bir teras. Taş mimarisini geniş bir yaşam alanıyla buluşturan bir Kapadokya deneyimi.": "A private terrace opening onto the valley light, combining stone architecture with a generous living space in Cappadocia.",
    "1 çift kişilik yatak": "1 double bed",
    "1 çift kişilik yatak + 1 açılır yatak": "1 double bed + 1 sofa bed",
    "1 çift kişilik yatak + 2 tek kişilik yatak": "1 double bed + 2 single beds",
    "Klima": "Air conditioning",
    "Özel banyo": "Private bathroom",
    "Çalışma alanı": "Work area",
    "Deniz manzarası": "Sea view",
    "Balkon": "Balcony",
    "Kahve köşesi": "Coffee corner",
    "Aile yaşam alanı": "Family living area",
    "Doğal taş mimari": "Natural stone architecture",
    "Isıtma": "Heating",
    "Çay ve kahve": "Tea and coffee",
    "Özel teras": "Private terrace",
    "Vadi manzarası": "Valley view",
    "Oturma alanı": "Seating area"
};
const englishServices: Readonly<Record<string, {
    name: string;
    description: string;
}>> = {
    "BREAKFAST": {
        "name": "Breakfast",
        "description": "Breakfast is included for all guests for each night of your stay."
    },
    "WIFI": {
        "name": "Wi-Fi",
        "description": "Internet access in your room and shared areas is included."
    },
    "LUNCH": {
        "name": "Lunch",
        "description": "Charged per adult and child, for each night of your stay."
    },
    "DINNER": {
        "name": "Dinner",
        "description": "Charged per adult and child, for each night of your stay."
    },
    "VALET": {
        "name": "Valet and parking",
        "description": "Charged per night for one vehicle."
    },
    "TRANSFER": {
        "name": "Airport transfer",
        "description": "One-way private airport transfer, charged per reservation."
    }
};
/** Translate presentation labels; keep canonical API data and booking identities unchanged. */
export function catalogText(value: string, language: BookingLanguage): string {
    return language === 'en' ? englishText[value] || value : value;
}
export function serviceText(code: string, field: 'name' | 'description', fallback: string, language: BookingLanguage): string {
    return language === 'en' ? englishServices[code]?.[field] || fallback : fallback;
}
