# Case Study: Otel Rezervasyon Widget'ı ve Turizm CRM
d# Case Study: Otel Rezervasyon Widget'ı ve Turizm CRM

## 1. Genel bakış

Aday 5 gün içinde, bir otelin web sitesine gömülebilen rezervasyon widget'ı ile bu rezervasyonlardan beslenen basit bir misafir CRM'i geliştirir. Her şeyi bitirmek beklenmiyor; neyi neden önceliklendirdiğiniz, kodun kendisi kadar önemli.

| Başlık | Detay |
| --- | --- |
| Pozisyon | Software Developer (Turizm CRM ve Otel Widget) |
| Hedef seviye | Junior–Mid (yaklaşık 1,5 yıl deneyim) |
| Süre | 5 takvim günü, tahmini 16–20 saat efor |
| Teslim | Adayın kendi hesabında açacağı public GitHub reposu + 30 dakikalık teknik sunum |
| Teknoloji | Dil, framework ve veritabanı adaya bırakılır; Docker kullanımı zorunludur. Seçimler README'de kısaca gerekçelendirilir. |

## 2. Senaryo

Rota Teknoloji (kurgusal), butik ve orta ölçekli otellere direkt satış altyapısı sunan bir turizm teknolojisi şirketidir. Oteller satışlarının büyük kısmını komisyonlu OTA'lar üzerinden yapıyor ve direkt kanal payını artırmak istiyor. Ürün iki parçadan oluşur:

1. **Rezervasyon widget'ı:** Otelin kendi sitesine tek satır script ile eklenir. Misafir tarih ve kişi sayısını seçer, uygun odaları ve fiyatları görür, rezervasyon yapar.
2. **Misafir CRM'i:** Otel ekibi rezervasyonları, misafir profillerini ve misafirle ilgili notları tek ekrandan yönetir.

Ürün ilk olarak iki pilot otelde çalışacak, bu yüzden sistem baştan çok kiracılı (multitenant) olmalı. Seed veri olarak aşağıdaki oteller kullanılabilir:

| Otel | Oda tipi | Oda adedi | Maks. kişi | Gecelik fiyat (TRY) |
| --- | --- | --- | --- | --- |
| Lagos Beach Hotel, Antalya | Standart | 20 | 2 | 3.200 |
| Lagos Beach Hotel, Antalya | Deluxe Deniz Manzaralı | 15 | 3 | 4.800 |
| Lagos Beach Hotel, Antalya | Aile Suiti | 5 | 4 | 7.500 |
| Peri Stone Cave Hotel, Kapadokya | Mağara Oda | 8 | 2 | 5.500 |
| Peri Stone Cave Hotel, Kapadokya | Teras Süit | 2 | 3 | 9.000 |

Otel ve şirket adları kurgusaldır; fiyatlar örnek değerlerdir.

## 3. Kapsam

Zorunlu maddeler çalışır durumda olmalı; beklenen ve bonus maddeler zaman kalırsa yapılır. Bir maddeyi bilinçli olarak atlayıp README'de gerekçelendirmek, birkaç maddeyi yarım bırakmaktan iyidir.

## 4. Zorunlu

### 4.1. Widget

- **M1** Otel sitesine `<script src=".../widget.js" data-hotel-key="...">` gibi tek satırla eklenebilir. Host sayfanın CSS'i widget'ı, widget'ın CSS'i host sayfayı bozmaz; izolasyon yöntemi adaya bırakılır.
- **M2** Arama: giriş ve çıkış tarihi, yetişkin ve çocuk sayısı. Geçmiş tarih, ters tarih aralığı ve kapasite aşımı doğrulanır.
- **M3** Sonuç listesi: uygun oda tipleri, gecelik ve toplam fiyat, kalan oda sayısı.
- **M4** Rezervasyon formu: ad, soyad, e-posta, telefon, KVKK aydınlatma onayı ve ayrı bir pazarlama izni. Onay ekranında rezervasyon kodu gösterilir.

### 4.2. Rezervasyon servisi

- **M5** Müsaitlik API'si: tarih aralığındaki her gece için oda tipi bazında envanter hesaplanır.
- **M6** Rezervasyon oluşturma: aynı son odaya eşzamanlı iki istek geldiğinde overbooking olmaz. Bunu kanıtlayan bir test zorunludur.
- **M7** Idempotency: aynı `Idempotency-Key` ile tekrarlanan istek ikinci bir rezervasyon üretmez.
- **M8** Rezervasyon iptali; iptalde envanter geri açılır.
- **M9** Çok kiracılılık: her istek bir otele aittir; bir otelin verisi başka bir otelden görülemez.

### 4.3. CRM

- **M10** Rezervasyon olayları (oluşturuldu, iptal edildi) CRM'e asenkron ulaşır.
- **M11** Misafir profili: e-posta ve telefona göre eşleştirme yapılır, tekrar gelen misafir aynı profilde toplanır. Profilde toplam konaklama sayısı ve harcama görünür.
- **M12** CRM ekranı: aramalı ve sayfalı misafir listesi, misafir detayında rezervasyon geçmişi.

### 4.4. Altyapı

- **M13** Tüm bileşenler (widget, servisler, veritabanı, varsa mesajlaşma altyapısı) Docker container'ı olarak çalışır ve tek komutla (ör. `docker compose up`) ayağa kalkar. Docker dışında yerel kurulum gerekmez.

## 5. Beklenen

- **S1** Rezervasyon durum makinesi: `PENDING`, `CONFIRMED`, `CHECKED_IN`, `CHECKED_OUT`, `CANCELLED`, `NO_SHOW`. Geçersiz geçişler reddedilir.
- **S2** Onaylanan rezervasyon dış PMS'e (otel yönetim sistemi) bildirilir; PMS'in taklit edilmesi yeterlidir. PMS yavaş ya da hatalı olduğunda bildirim kaybolmaz.
- **S3** CRM'de misafire not ve etiket ekleme (VIP, balayı, şikayet gibi).
- **S4** Widget'ta Türkçe ve İngilizce dil desteği.
- **S5** Container imajları üretime uygundur: çok aşamalı build, root olmayan kullanıcı, health check ve bağımlılık sırasına göre başlatma.
- **S6** Müsaitlik sorgusu için cache stratejisi ve rezervasyon anında invalidasyonu.

## 6. Bonus

- **B1** CRM'de segment oluşturma: örneğin "son 12 ayda 2+ konaklama" ya da "30 gün önce check-out yapmış ve pazarlama izni olan".
- **B2** Widget huni olayları (arama, oda seçimi, form, onay) ve CRM'de basit dönüşüm oranı.
- **B3** Fiyat kuralları: hafta sonu farkı, minimum konaklama, TRY ve EUR gösterimi.
- **B4** Widget isteğinden CRM consumer'ına kadar taşınan trace id ve temel metrikler.
- **B5** Widget bundle boyutunun ölçülüp README'de raporlanması.
- **B6** Seçilen bir çalışma ortamı için dağıtım yapılandırması.

## 7. Teknik beklentiler ve kısıtlar

- **Mimari serbesttir:** modüler monolit de mikroservis de kabul edilir, yeter ki Rezervasyon, Envanter ve Misafir (CRM) sınırları kodda net görünsün.
- **Teknoloji seçimi:** Dil, framework, veritabanı, mesajlaşma ve test araçları adaya bırakılır; tek zorunluluk tüm sistemin Docker ile çalışmasıdır (M13). Her seçim README'de kısaca gerekçelendirilir.
- **Kod yapısı:** Domain mantığı API ve veri erişim katmanından ayrı olmalı.
- **Asenkron iletişim:** Olay şeması, olayların sırası ve tekrar işlenen mesaj durumu README'de açıklanır.
- **Güvenlik:** Widget herkese açık bir otel anahtarıyla çalışır (gizli değildir). CRM uçları kimlik doğrulama ister; basit bir yöntem yeterlidir. CORS yalnızca otelin tanımlı domain'lerine açıktır.
- **Test:** Domain kuralları için unit, API için integration test. M6'daki eşzamanlılık testi zorunludur.
- **Kapsam dışı:** Ödeme entegrasyonu yoktur ("ödeme otelde" varsayılır). E-posta gönderimi log'a yazılabilir. Gerçek PMS yoktur, taklit edilmesi yeterlidir.
- **Yapay zekâ araçları:** Kullanılabilir. README'de nerede kullanıldığını belirtin; sunumda her satırı açıklayabilmeniz beklenir.

Rezervasyon servisi CRM'i doğrudan çağırmaz; CRM, rezervasyon olaylarından beslenir. Teknoloji seçimleri ve servis sınırları adayın kararıdır.

*sistem bağlamı · 6 bileşen*

## 8. Teslimatlar

Teslim, kendi GitHub hesabınızda açtığınız public reponun linkini 5. günün sonuna kadar paylaşmanızla tamamlanır; ardından 30 dakikalık sunum planlanır.

1. **Public GitHub reposu:** Kendi hesabınızda açılmış, anlamlı ve adım adım ilerleyen commit geçmişi olan bir repo. Değerlendirme süreci bitene kadar public kalmalı.
2. **README:** Kurulum (tek komut), mimari özeti, alınan kararlar ve alternatifleri, bilinen eksikler, "bir hafta daha olsaydı ne yapardım".
3. **En az iki ADR:** Örneğin widget izolasyon yöntemi ve overbooking önleme yöntemi.
4. **API dokümantasyonu:** format serbest.
5. **Seed veri:** Senaryodaki iki otel ve oda tipleri.
6. **Demo videosu (isteğe bağlı):** 3–5 dakika.

## 9. Sunum akışı

10 dakika demo, 20 dakika kod ve tasarım üzerine tartışma.
