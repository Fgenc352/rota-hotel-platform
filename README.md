# Rota — Otel rezervasyonu ve misafir CRM

## Docker ile çalıştırma

Docker açıkken proje kökünde:

```bash
docker compose up
```

Kod değiştiyse `docker compose up --build` kullan. Frontend, widget, backend, PostgreSQL, Kafka, Redis ve dış PMS birlikte açılır. Docker demosu için Java, Maven veya Node kurulumu gerekmez.

## Otel siteleri — Docker

| Tasarım | Lagos | Peri |
| --- | --- | --- |
| Showcase | http://localhost:4200/showcase/lagos-beach | http://localhost:4200/showcase/peri-stone |
| Signature | http://localhost:4200/signature/lagos-beach | http://localhost:4200/signature/peri-stone |
| Atelier | http://localhost:4200/atelier/lagos-beach | http://localhost:4200/atelier/peri-stone |
| Maison | http://localhost:4200/maison/lagos-beach | http://localhost:4200/maison/peri-stone |
| Quiet | http://localhost:4200/quiet/lagos-beach | http://localhost:4200/quiet/peri-stone |

## Otel siteleri — Yerel

| Tasarım | Lagos | Peri |
| --- | --- | --- |
| Showcase | http://localhost:4205/showcase/lagos-beach | http://localhost:4205/showcase/peri-stone |
| Signature | http://localhost:4205/signature/lagos-beach | http://localhost:4205/signature/peri-stone |
| Atelier | http://localhost:4205/atelier/lagos-beach | http://localhost:4205/atelier/peri-stone |
| Maison | http://localhost:4205/maison/lagos-beach | http://localhost:4205/maison/peri-stone |
| Quiet | http://localhost:4205/quiet/lagos-beach | http://localhost:4205/quiet/peri-stone |

## Login, CRM, PMS operasyonları ve Swagger

| Ekran | Docker | Yerel |
| --- | --- | --- |
| Login | http://localhost:4200/login | http://localhost:4205/login |
| Misafirler | http://localhost:4200/crm/guests | http://localhost:4205/crm/guests |
| Rezervasyonlar | http://localhost:4200/crm/reservations | http://localhost:4205/crm/reservations |
| PMS operasyonları | http://localhost:4200/crm/pms | http://localhost:4205/crm/pms |
| Genel bakış | http://localhost:4200/crm/operations | http://localhost:4205/crm/operations |
| Swagger | http://localhost:8080/swagger-ui/index.html | http://localhost:8085/swagger-ui/index.html |

**Giriş:** Kullanıcı adı `lagos` veya `peri`, parola `DemoHotel2026!`. CRM ve PMS operasyon ekranları giriş gerektirir. Profil ve rezervasyon detayları listelerden açılır.

## Widget’lar — Lagos

| Tasarım | Docker | Yerel |
| --- | --- | --- |
| Classic | http://localhost:4300/widget?hotel=lagos-beach | http://localhost:4305/widget?hotel=lagos-beach |
| Premium | http://localhost:4300/widget-premium?hotel=lagos-beach | http://localhost:4305/widget-premium?hotel=lagos-beach |
| Signature | http://localhost:4300/widget-signature?hotel=lagos-beach | http://localhost:4305/widget-signature?hotel=lagos-beach |
| Atelier | http://localhost:4300/widget-atelier?hotel=lagos-beach | http://localhost:4305/widget-atelier?hotel=lagos-beach |
| Maison | http://localhost:4300/widget-maison?hotel=lagos-beach | http://localhost:4305/widget-maison?hotel=lagos-beach |
| Quiet | http://localhost:4300/widget-quiet?hotel=lagos-beach | http://localhost:4305/widget-quiet?hotel=lagos-beach |

## Widget’lar — Peri

| Tasarım | Docker | Yerel |
| --- | --- | --- |
| Classic | http://localhost:4300/widget?hotel=peri-stone | http://localhost:4305/widget?hotel=peri-stone |
| Premium | http://localhost:4300/widget-premium?hotel=peri-stone | http://localhost:4305/widget-premium?hotel=peri-stone |
| Signature | http://localhost:4300/widget-signature?hotel=peri-stone | http://localhost:4305/widget-signature?hotel=peri-stone |
| Atelier | http://localhost:4300/widget-atelier?hotel=peri-stone | http://localhost:4305/widget-atelier?hotel=peri-stone |
| Maison | http://localhost:4300/widget-maison?hotel=peri-stone | http://localhost:4305/widget-maison?hotel=peri-stone |
| Quiet | http://localhost:4300/widget-quiet?hotel=peri-stone | http://localhost:4305/widget-quiet?hotel=peri-stone |

## Ortak dış PMS mock — Docker ve yerel uygulama için aynı

| Ekran | Adres |
| --- | --- |
| PMS mock paneli | http://localhost:8090 |
| Gelen rezervasyonlar | http://localhost:8090/api/reservations |
| İstek geçmişi | http://localhost:8090/api/requests |
| Durum ve sayaçlar | http://localhost:8090/api/status |

Docker ve yerel backend aynı dış PMS mock’una gönderir. Yukarıdaki CRM içi PMS operasyon ekranı oda atama/temizlik/günlük hizmetler içindir; bu dış PMS paneli gelen bildirimleri gösterir.

## Yerelde çalıştırma

Docker ile yerel uygulamalar aynı anda açık kalabilir; portları farklı, verileri ortaktır.

1. Docker servislerini `docker compose up` ile aç. Zaten açıksa tekrar çalıştırmana gerek yok.
2. IntelliJ’de `HotelApplication` çalıştır; **Active profiles alanını boş bırak**.
3. `frontend` klasöründe ilk kurulum için `npm ci`, ardından `npm start` çalıştır. Yerel frontend `4205`, backend `8085` portunu kullanır.
4. Ayrı widget sunucusu için başka bir `frontend` terminalinde `npm run start:widget` çalıştır: port `4305`. Otel sayfalarındaki widget’lar `npm start` ile de açılır.
5. Dış PMS Docker’da ortaktır; `npm run start:pms` çalıştırmana gerek yok. Daha önce IntelliJ’ye `--app.pms.url=http://localhost:8095` veya `APP_PMS_URL` eklediysen kaldır.

Backend tek `application.yml` kullanır; Kafka ve Redis varsayılan olarak etkindir. Docker kendi adres ve portlarını Compose üzerinden verir. İki backend aynı DB, Kafka ve Redis’i kullanır; birinde yaptığın işlem diğerinde de görünür, ekranı yenilemen gerekebilir. Oturum cookie adları farklıdır, böylece iki arayüze birlikte giriş yapılabilir.

**Veriler yeniden başlatmada korunur.** Liquibase yalnız yeni değişiklikleri uygular (`drop-first: false`); Hibernate şemayı doğrular (`ddl-auto: validate`). Demo kayıtları ilk kurulumda eklenir.

Durum: `docker compose ps` · Loglar: `docker compose logs -f` · Kapatma: `Ctrl+C` veya `docker compose down`. Normal `down` volume’ları korur.

<details>
<summary>Widget scriptleri, API ve altyapı bağlantıları</summary>

### Widget’ı başka siteye ekleme

```html
<script src="http://localhost:4300/widget-atelier.js" data-hotel-key="lagos-beach"></script>
```

Peri için `data-hotel-key="peri-stone"` kullan. Diğer tasarımların scriptleri:

| Tasarım | Docker scripti | Yerel script |
| --- | --- | --- |
| Classic | http://localhost:4300/widget.js | http://localhost:4305/widget.js |
| Premium | http://localhost:4300/widget-premium.js | http://localhost:4305/widget-premium.js |
| Signature | http://localhost:4300/widget-signature.js | http://localhost:4305/widget-signature.js |
| Atelier | http://localhost:4300/widget-atelier.js | http://localhost:4305/widget-atelier.js |
| Maison | http://localhost:4300/widget-maison.js | http://localhost:4305/widget-maison.js |
| Quiet | http://localhost:4300/widget-quiet.js | http://localhost:4305/widget-quiet.js |

`widget-gallery.js` galeri açılırken otomatik yüklenen yardımcı scripttir; ayrıca eklenmez. Docker’daki otel sayfaları kendi widget yollarını ayrı widget servisine proxy eder. Eski `/hotels/lagos-beach` ve `/hotels/peri-stone` yolları Signature sayfalarına yönlenir.

### API ve sağlık kontrolleri

| Amaç | Docker | Yerel |
| --- | --- | --- |
| Canlı OpenAPI JSON | http://localhost:8080/v3/api-docs | http://localhost:8085/v3/api-docs |
| Backend sağlık kontrolü | http://localhost:8080/actuator/health | http://localhost:8085/actuator/health |
| Lagos oda kataloğu | http://localhost:8080/api/public/hotels/lagos-beach/rooms | http://localhost:8085/api/public/hotels/lagos-beach/rooms |
| Peri oda kataloğu | http://localhost:8080/api/public/hotels/peri-stone/rooms | http://localhost:8085/api/public/hotels/peri-stone/rooms |
| Lagos hizmet kataloğu | http://localhost:8080/api/public/hotels/lagos-beach/services | http://localhost:8085/api/public/hotels/lagos-beach/services |
| Peri hizmet kataloğu | http://localhost:8080/api/public/hotels/peri-stone/services | http://localhost:8085/api/public/hotels/peri-stone/services |

Container sağlık kontrolleri: http://localhost:4200/healthz · http://localhost:4300/healthz · http://localhost:8090/health.

Repodaki sözleşme: [doc/openapi.json](doc/openapi.json). Swagger’da yetkili istekleri **Authorize** ile ilgili otelin hesabını kullanarak deneyebilirsin.

### Altyapı

| Altyapı | Bağlantı | Bilgi |
| --- | --- | --- |
| PostgreSQL | `localhost:5432` | DB: `hotel`, şema: `public`, kullanıcı: `hotel`, parola: `hotel-local-only` |
| Kafka | `localhost:9092` | Kafka istemci adresi; HTTP sayfası değildir. |
| Redis | `localhost:6379` | Redis istemci adresi; HTTP sayfası değildir. |

</details>

## Dokümana göre yapılanlar

Kodda uygulanmış kapsam; tüm Docker/tarayıcı kabulünün geçtiği anlamına gelmez:

| Maddeler | Yapılan |
| --- | --- |
| M1–M4 | Script/iframe widget, tarih/kişi doğrulama, oda/fiyat/stok, izinler ve rezervasyon onayı. |
| M5–M9 | Gecelik envanter, eşzamanlılık testleri, idempotency, iptal ve otel bazlı veri ayrımı. |
| M10–M12 | Asenkron CRM, e-posta/telefonla otomatik profil eşleştirme, arama ve sayfalama. |
| M13 | Tek `docker compose up` ile tüm servislerin açılacağı yapılandırma. |
| S1–S6 | Durum makinesi, PMS retry, not/etiket, TR/EN, Docker sağlık kontrolleri ve cache. |
| B1–B4 | Segment, huni/dönüşüm, fiyat kuralları/TRY-EUR ve trace/temel metrikler. |
| Teslim | README, iki ADR, OpenAPI/Swagger ve iki otelin seed verileri. |

Açık kabul işleri: gerçek Docker açılışı, PostgreSQL eşzamanlılık kanıtı, Kafka/Redis ve tarayıcı testlerinin başarılı raporları. Public repo/commit geçmişi ve sunum teslim sahibince tamamlanır. B5 için bundle ölçüm scripti mevcut; B6 uzak deploy kapsam dışında.

## Mimari ve teknoloji seçimleri

Backend, Spring Boot 3.5 / Java 25 ile modüler monolittir. `hotel`, `inventory`, `reservation`, `guest`, `identity`, `events`, `analytics` ve `pms` modülleri iş alanlarına göre ayrılır. Her modülde API, uygulama ve veri erişimi sorumlulukları ayrıdır; iş kuralları domain tarafındadır. Komutlar yazma, sorgular okuma işlemlerini yürütür. Bu CQRS ayrımı ayrı veritabanı veya event sourcing gerektirmez.

| Seçim | Gerekçe ve değerlendirilen alternatif |
| --- | --- |
| Spring Boot, Java, Hibernate/JPA, Lombok | Transaction ve erişim kontrolünü ortak altyapıyla yönetmek; mapper/entity ayrımıyla iş kurallarını persistence ayrıntısından korumak. Kritik PostgreSQL kilit/UPSERT işlemlerinde SQL kullanılır. Tam JDBC daha fazla elle eşleme gerektirirdi. |
| PostgreSQL | Gecelik envanter, benzersiz anahtarlar, transaction ve satır kilitleri. Sadece uygulama belleğinde kilit birden fazla backend instance'ında yeterli olmazdı. |
| Liquibase | Şema ve pilot verileri sürümlü, tekrar uygulanabilir değişikliklerle kurmak. Hibernate yalnız şemayı doğrular; şema üretmez. |
| Transactional outbox | Rezervasyon ile olay aynı transaction'da yazılır. Doğrudan CRM/HTTP çağrısı yapmak dış sistem hatasını rezervasyona bağlardı. |
| Kafka ve Redis | Kafka dış PMS'e giden olayların aktarım katmanıdır; Redis instance'lar arasında müsaitlik cache'ini paylaşır. Küçük sistem için zorunlu değiller; doğrudan PMS + bellek cache modu da korunur. |
| Angular 21 / TypeScript | Formlar, durum yönetimi, lazy yüklenen ekranlar ve paylaşılan rezervasyon bileşenleri. Altı widget tasarımı aynı rezervasyon mantığını kullanır. |
| Docker Compose / Nginx | Yedi servisi tek komutla çalıştırmak. Frontend ve widget ayrı container'lardır; aynı frontend build çıktısını kullanırlar. Bu, iki ayrı frontend kod tabanı gerektirmez. |
| JUnit, Mockito, Testcontainers, WireMock, Node test, Playwright | Domain kararları, gerçek PostgreSQL/Kafka/Redis, HTTP hata senaryoları ve tarayıcı etkileşimlerini farklı seviyelerde sınamak. |

Akış: otel sitesi → widget iframe → public API → rezervasyon + gecelik stok + outbox transaction'ı → bağımsız CRM consumer'ı ve PMS aktarımı. CRM ve PMS operasyon ekranları yetkili personelin otel kimliğini kullanır. Public otel anahtarı bir parola değildir. Oteller aynı veritabanındadır; sorgular ve ilişkiler `hotel_id` ile sınırlandırılır.

Kararlar: [ADR 001 — Widget izolasyonu](doc/adr/001-widget-izolasyonu.md), [ADR 002 — Gecelik stok ve kilitleme](doc/adr/002-gecelik-envanter.md).

## Otomatik misafir profili

Aynı otelde e-posta veya telefon eşleşiyorsa rezervasyon mevcut profile eklenir. Yeni e-posta/telefon eskisini silmez; `guest_identifier` içinde ek iletişim bilgisi olarak saklanır. Profil detayında tüm bilinen e-postalar ve telefonlar gösterilir. Her iki bilgi de tamamen farklıysa yeni profil oluşur. Ad/soyad benzerliği kullanılmaz.

Bir kayıt daha önce ayrı oluşmuş profillerin iletişim bilgilerini birbirine bağlıyorsa sistem bunları otomatik tek profilde toplar. Hedef, eşleşen aktif profil kimliklerinin DB sıralamasındaki ilkidir; diğer profillerin rezervasyonları, notları, etiketleri ve iletişim eşleştirmeleri bu hedefe taşınır. Eski profil silinmez, `merged_into` ile bağlanır. Notların yazar ve eklenme tarihleri değişmez. Sonraki durum olayları da taşınmış rezervasyonun bağlı olduğu profili kullanır.

Personelin profil seçme/birleştirme ekranı ve `/matching` endpoint'leri kaldırıldı. Otomatik birleştirme tek transaction içindedir; otel kilidi paralel eşleştirmeleri sıraya alır. `guest_match_audit`, kaynak/hedefin önceki ve sonraki hallerini `SYSTEM_CONTACT_MATCH` aktörüyle tutar. Önceden uygulanmış migration ve audit geçmişi korunur.

## Segment oluşturma

Özel segment ölçütleri: **minimum tamamlanmış konaklama**, **son kaç ay**, **pazarlama izni gerekli mi**. Opsiyonel “kaç gün önce çıkış” alanı kaldırıldı. Yeni Liquibase değişikliği eski `days_since_checkout` sütununu kaldırır; kaydedilmiş segmentlerin diğer ayarları ve kayıtları korunur. Hazır `RETURNING` ve `REENGAGE` segmentleri kendi sabit kurallarını kullanmaya devam eder.

## Misafir notları

Notlar, eklenme anından itibaren ilk 24 saat içinde yalnız notu yazan personel tarafından silinebilir. Profilde “Notu sil” seçeneği ve onay adımı vardır. Tam 24 saat dolduğunda buton devre dışı kalır; API de `409 NOTE_DELETE_WINDOW_CLOSED` döndürür. Başka personel `403`, başka otel/profil veya bulunamayan not `404` alır. Süre sunucuda UTC anlarıyla kontrol edilir; profil eşleştirmesinde taşınan notun özgün oluşturulma tarihi korunur. Yeni tablo veya migration gerekmez.

API: `DELETE /api/crm/guests/{id}/notes/{noteId}`; başarılı sonuç `204`. Silme işlemi not içeriği yazılmadan otel, profil, not kimliği ve personel bilgisiyle uygulama loguna kaydedilir.

## Cache tutarlılığı

Cache anahtarı otel, giriş/çıkış, yetişkin/çocuk sayısı ve otelin rezervasyon sürümlerinin toplamını kullanır. Rezervasyon oluşturma ve durum değişiklikleri sürümü değiştirir; eski cache sonucu yeni anahtarla kullanılmaz. Redis kesilirse veritabanından hesaplanır. Bellek cache'i de aynı sürüm kontrolünü kullanır. TTL varsayılan olarak 5 saniyedir.

Doğrudan SQL ile fiyat veya katalog değiştirmek rezervasyon sürümünü artırmaz; böyle bir değişiklikte mevcut cache kaydı TTL dolana kadar eski sonuç döndürebilir. Katalog değişiklikleri için ayrı sürümleme/geçersizleştirme henüz uygulanmamıştır.

## Olay sözleşmesi, sıra ve tekrarlar

`outbox_event` alanları: `id`, `hotel_id`, `reservation_id`, `aggregate_version`, `event_type`, `schema_version`, `payload`, `trace_id`, `created_at`. `schema_version` mesaj yapısını, `aggregate_version` tek rezervasyonun değişiklik sırasını ifade eder.

Olay türleri `ReservationCreated`, `ReservationCancelled`, `ReservationStatusChanged`. Payload, `ReservationSnapshot` anlık görüntüsüdür: rezervasyon/otel kimliği, kod, tarihler, misafir iletişim bilgileri ve izin zamanı, durum, TRY toplamı, sürüm ve gerçekleşen çıkış zamanı. İletişim bilgileri DB outbox/CRM içinde kalır; Kafka mesajı bunları içermez.

Varsayılan Docker akışında:

1. Rezervasyon, stok, idempotency cevabı ve outbox birlikte commit edilir. API, CRM/PMS sonucunu beklemez.
2. CRM consumer'ı outbox'tan okur; profil güncellemesi ile teslim onayı birlikte commit edilir.
3. Kafka publisher'ı `eventId`, `hotelId`, `reservationId`, `aggregateVersion`, `type`, `traceId` alanlarını yayınlar. Partition anahtarı `reservationId`'dir. Broker onayı gelmeden outbox teslimi tamamlandı sayılmaz.
4. Kafka listener'ı mesajı kalıcı outbox ile doğrular ve tekil PMS teslim kaydı açar. Bu case içinde aynı backend/veritabanına bağlıdır; bağımsız bir dış servis sözleşmesi değildir.
5. PMS worker'ı kalıcı teslim kaydından HTTP isteği gönderir. `Idempotency-Key` olarak sabit event ID kullanılır. PMS mesajı yalnız gerekli rezervasyon kimliği, kod, tarihler, durum, sürüm ve çıkış zamanını içerir.

Doğrudan modda Kafka adımları atlanır; outbox PMS'e HTTP teslimini yine yönetir. CRM iki modda da kendi outbox consumer'ıyla ilerler.

Sıra tek rezervasyon içindir; oteller veya farklı rezervasyonlar arasında global sıra vaat edilmez. Önceki rezervasyon sürümü işlenmeden sonraki teslim seçilmez. CRM uygulanmış sürümü kontrol eder: tekrarları yok sayar, sürüm boşluğunu reddeder. Kafka inbox benzersiz `(event_id, consumer)` kaydıyla tekrar teslimleri çoğaltmaz. PMS aynı event ID'yi tekrar kaydetmez ve eski sürümle yeni durumu ezmez.

Teslimat **en az bir kez** yapılır; exactly-once iddiası yoktur. HTTP isteği başarılı olup yerel onay yazılamazsa aynı olay tekrar gönderilebilir. Dış çağrı sırasında DB satır kilidi tutulmaz. Başarısız teslimler artan beklemeyle yeniden denenir; 10 başarısız denemeden sonra CRM operasyon ekranından manuel tekrar deneme gerekir. Kafka'daki bozuk mesajlar sınırlı denemeden sonra DLT'ye gider; kalıcı altyapı hataları ayrıca retry politikasına tabidir.

`X-Trace-Id`, API'den outbox'a, consumer loglarına ve PMS isteğine taşınır. Temel HTTP/consumer sayaçları vardır; mevcut dış Actuator erişimi yalnız health içindir, metrics endpoint'i açık değildir.

## Test ve canlı kabul

Uygulama kaynaklarını yerelde test etmek için Java 25, Maven ve Node 24 gerekir; yalnız Docker demosu için bunlar gerekmez.

```bash
mvn clean install
cd frontend
npm ci
npm test
npm run build
npm run test:pms
npm run e2e
```

`mvn clean install` tüm backend testlerini çalıştırır. Testcontainers ayrı DB/Kafka/Redis açar; demo DB'yi kullanmaz. WireMock JSON senaryoları `src/test/resources/wiremock/pms` altındadır. Playwright testlerinin bir kısmı API fixture'ları kullanır; bunlar gerçek backend kanıtı sayılmaz. `cross-origin-embed.spec.mjs` gerçek public API ister. Playwright varsayılan olarak yerel `4205` adresini kullanır. Docker frontend'ini sınamak için Linux/macOS terminalinde `E2E_BASE_URL=http://localhost:4200 npm run e2e`; PowerShell'de önce `$env:E2E_BASE_URL="http://localhost:4200"`, sonra `npm run e2e` çalıştır.

Container'lar açıldıktan sonra proje kökünde `node scripts/docker-smoke.mjs`, widget sunucusunu, altı loader'ı/iki otel rotasını, API proxy'lerini, rezervasyon/idempotency, oturum/CSRF ve dış PMS teslimini sınar. Tarayıcı etkileşimleri ayrıca Playwright ile kontrol edilir. CI iki backend modunda container smoke çalıştırır; backend test raporlarını ve tarayıcı/log çıktılarını artifact olarak toplar.

Profil ve segment sadeleştirmesinde 141 backend unit testi ve 71 frontend senaryosu geçti; frontend production build başarılı oldu. PMS depo kontrollerinin son koşusunda 10 test geçti. Gerçek PostgreSQL functional testleri Docker erişimi nedeniyle; Playwright senaryoları web sunucusu başlatılamadığı için çalıştırılamadı. Kendi bilgisayarında Docker/Kafka/Redis, gerçek API ve tarayıcı kabulünü ayrıca çalıştır.


## Bilinen eksikler ve bir hafta daha olsaydı

- Fiyat, rezervasyon oluşturulurken sunucuda yeniden hesaplanır. Misafir formu doldururken katalog fiyatı değişirse yeni tutar için ayrıca onay alınmıyor. Sonraki öncelik, süreli teklif/sürüm ve fiyat değişiminde yeniden onay akışı olurdu.
- Oda adedini SQL'den değiştirmek fiziksel PMS odalarını ve mevcut gecelik `capacity` değerlerini otomatik güncellemez. Kapasite düzenleme işlemi, satılmış stok sınırı ve fiziksel odalar birlikte yönetilmelidir. Mevcut cache yalnız rezervasyon sürümü ve TTL kullanır; doğrudan katalog değişikliklerini anında geçersizleştirmez.
- DB ve dış PMS verileri yeniden başlatmalarda korunur. Normal `docker compose down` volume'ları silmez; veri kaybetmemek için `down -v` kullanma.
- İkinci öncelik, tam Docker/Windows kabulü, yük testleri, DB/cache sorgu ölçümleri ve outbox/arşiv saklama politikası olurdu. Gözlemlenebilirlik için yetkili metrics erişimi ve dashboard eklenebilirdi.

## AI kullanımı

Bu projede AI/Codex; backend/frontend geliştirme ve yeniden düzenleme, test senaryoları, hata inceleme, UI tasarım varyasyonları ve dokümantasyon hazırlığında kullanıldı.
