# KTerm Terminal (Android)

Siyah arka planlı, yeşil monospace yazılı, dokunmatik kullanıma uygun Android terminal arayüzü.

## Neler çalışıyor?
- Android'in `/system/bin/sh` kabuğunda gerçek komut çalıştırma
- `help`, `clear`, `pwd`, `cd` komutları
- Komut çıktısı ve hata çıktısını gösterme
- Her komut için 8 saniye sınırı; takılan komut durdurulur
- Root ve geniş dosya erişim izni istemez

## Sınırlamalar
Bu, Termux'un tam Linux ortamı değildir. Android'in kendi kabuğunu ve cihazda erişime açık komutları kullanır. `pkg`, `apt`, Python/Node paket ortamı ve root erişimi bu projeye dahil değildir. Android uygulama izinleri nedeniyle dosya erişimi sınırlıdır.

## APK oluşturma
`.github/workflows/build-apk.yml` GitHub Actions ile APK derlemesini otomatikleştirir. Proje dosyalarını bir GitHub deposuna yükleyip `Actions` sekmesinden **Build KTerm APK** iş akışını çalıştırın. Başarılı derleme sonunda `KTerm-Android-APK` isimli artifact içinden `app-debug.apk` dosyasını indirebilirsiniz.

Yerel derleme için Android SDK (API 35), Java 17 ve Gradle 8.7 gerekir. Komut: `gradle assembleDebug`.
