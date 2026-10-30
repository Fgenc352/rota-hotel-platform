# ADR 001 — Widget'ı iframe ile izole etmek

Durum: Kabul edildi.

## Bağlam

Widget tek script ile farklı otel sitelerine eklenebilmeli. Host CSS'i rezervasyon formunu, widget CSS'i host sayfayı bozmamalı. Altı görsel tasarım aynı rezervasyon davranışını kullanıyor.

## Karar

Loader scripti, kendi origin'inde otel anahtarıyla bir iframe oluşturur. Form, dil/fiyat seçimi ve API istekleri iframe içinde çalışır. Docker'da widget Nginx servisi `4300`, otel siteleri/CRM Nginx servisi `4200` portundadır. Aynı derlenmiş Angular bileşenleri paylaşılır; widget hostu yalnız public rezervasyon/galeri rotalarını ve public API proxy'sini sunar.

Yerel geliştirmede frontend `4205`, isteğe bağlı ayrı widget sunucusu `4305` portunu kullanır. Docker ve yerel frontend aynı backend işlevlerini farklı portlardan çağırır; rezervasyon verisi ortak DB'dedir.

Otel sayfaları kendi origin'indeki widget yollarını ayrı widget servisine proxy eder. Dış siteler scripti doğrudan widget origin'inden yükler. Personel girişi/CRM widget hostunda sunulmaz; backend yetkilendirmesi her durumda geçerlidir.

Boyut, rezervasyon onayı ve galeri mesajları `postMessage` ile taşınır. Loader alıcıları `origin` ve `source` kontrolü yapar; mesaj alanları doğrulanır. İframe `sandbox` izinleri script/form/same-origin ihtiyaçlarıyla sınırlandırılır. Tam ekran galeri hostta geçici bir dialog ve ayrı iframe kullanır; stiller kendi seçicisine aittir, kapanınca host scroll stili geri yüklenir.

## Alternatifler

Shadow DOM, form CSS'ini ayırır ancak host JavaScript'i ve tam ekran davranışlarıyla daha fazla ortak alan bırakır. Normal DOM'a doğrudan bileşen eklemek host CSS'ine bağımlılığı artırır. Her otel için ayrı widget kodu iş kurallarını tekrar ederdi.

## Sonuçlar

Host stillerinden güçlü ayrım ve bağımsız widget sunucusu kazanılır. Buna karşılık iframe yüksekliği, galeriler, scroll, erişilebilirlik ve cross-origin mesajlaşma açıkça yönetilmelidir. İframe içindeki public API isteğinin origin'i widget sunucusudur; bu, host sitenin kimliğini doğrulamaz. Public otel anahtarı gizli değildir. Doğrudan public API CORS kontrolleri otelin tanımlı domain'lerine göre ayrıca uygulanır.

Kanıt: loader birim testleri, galeri/mesaj testleri, production build; canlı CSS/cross-origin kabulü henüz tamamlanmadı. Çalıştırma ve doğrulama adımları [README](../../README.md) içindedir.
