# ADR 002 — Gecelik envanter satırlarını kilitlemek

Durum: Kabul edildi.

## Bağlam

Aynı son odaya iki eşzamanlı rezervasyonda tek satış yapılmalı. Farklı tarihlerdeki aynı oda tipi rezervasyonları ortak oda tipi kilidinde beklememeli. Oda sayısı birden fazlaysa her rezervasyon yalnız bir birim tüketmeli.

## Karar

Satılabilir stok `(hotel_id, room_type_id, stay_date)` anahtarlı `room_inventory` satırlarıdır. Aralık `[check_in, check_out)` olarak ele alınır. Çıkış gecesi tüketilmez. Fiziksel oda numarası PMS'te ayrıca atanır; rezervasyon sırasında tek bir fiziksel oda ayrılması zorunlu değildir.

Rezervasyon transaction'ı geceleri artan tarih sırasıyla işler. Olmayan satır `INSERT ... ON CONFLICT DO NOTHING` ile güvenli oluşturulur. İlgili gece `SELECT ... FOR UPDATE` ile kilitlenir; `sold < capacity` ise sayaç bir artırılır. Kilit transaction sonuna kadar tutulur. İlk kez oluşturulan aynı gece için yarış da benzersiz anahtar/UPSERT ile bekletilir. Oda tipi kaydında genel yazma kilidi yoktur.

İptal/no-show, önce rezervasyon durumunu kilitleyip geçişi doğrular; gecelik stok iadesini aynı tarih sırasıyla yapar. Tekrarlanan durum isteği yeniden iade yapmaz. Herhangi bir gece doluysa tüm transaction geri alınır: önceki geceler, rezervasyon, idempotency cevabı ve outbox yarım kalmaz.

`CHECK(sold >= 0 AND sold <= capacity)` son DB savunmasıdır. Aynı gecede birden fazla boş oda olduğunda stok artırımları kısa süreyle sıraya girer; bu, bütün odaların iş açısından rezerve edildiği anlamına gelmez. Farklı gecelerde bu envanter kilidi paylaşılmaz.

## Ortak veritabanı ve kalıcılık

Docker ve IntelliJ backend'leri aynı PostgreSQL `hotel/public` tablolarını kullanır. Kilitler DB'de olduğu için iki farklı JVM'den gelen rezervasyonlar da aynı stok üzerinde koordine edilir. Kafka topic/group ve Redis cache alanı ortaktır; port farkı veri ayrımı yaratmaz. Personel otel yetkisi her backend'de ayrıca kontrol edilir.

Her iki çalışma şekli tek `application.yml` kullanır; aktif profil gerekmez. Kafka ve Redis varsayılan olarak etkindir. Docker container adreslerini ve portunu Compose ortam değişkenleriyle verir.

Her iki çalışma şeklinde `spring.liquibase.drop-first: false`, `spring.jpa.hibernate.ddl-auto: validate` kullanılır. Başlangıçta yalnız yeni changeset'ler uygulanır; mevcut kayıtlar ve uygulanmış seed verileri korunur. Liquibase değişiklik kilidi aynı DB'ye gelen migration çalıştırmalarını sıraya alır. Mevcut changeset'ler değiştirilmez; şema güncellemesi yeni changeset ile yapılır. Hibernate'in `update/create` seçeneği migration geçmişinin yerine kullanılmaz.

## Alternatifler

Oda tipi kaydını kilitlemek güvenli fakat ilgisiz tarihleri de bekletirdi. Yalnız Java kilidi birden fazla instance'ta korumaz. Optimistic locking ve retry yapılabilirdi; çok geceli işlemlerde tekrar yükü ve geri alma yönetimi artardı. Fiziksel oda başına rezervasyon yapmak daha ayrıntılı planlama gerektirir; PMS atamasıyla ayrıca ele alınır.

## Sonuçlar

Gecelik stok ve tenant ayrımı korunur; fazla satış önlenir. Çakışan günlerin artan sırayla kilitlenmesi envanter deadlock riskini azaltır. Satış için veritabanı otoritedir; cache satış izni vermez. Fiziksel oda ataması kendi çakışma/kilit kontrollerine sahiptir.

Testler: `BookingFunctionalTest.simultaneousLastRoomHasExactlyOneWinner`, çok geceli rollback ve tek iade senaryoları; `RepositoryIntegrationTest` farklı tarihlerin beklememesi ve 2 oda için 3 yarışan istekte yalnız 2 kabul kontrolü.
