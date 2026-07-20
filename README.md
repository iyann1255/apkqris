# QRISPay Notif Reader (Android)

Aplikasi pembaca notifikasi pembayaran (mirip Casaku). Membaca notifikasi
e-wallet / m-banking di HP, lalu meneruskan teksnya ke webhook QRISPay.
Server yang mengekstrak nominal dan mencocokkan transaksi → auto LUNAS.

## Cara pakai (setelah APK terpasang)
1. Buka aplikasi, isi **Server URL** (mis. `http://43.159.49.150`) dan **License Key** (dari dashboard).
2. Tekan **Simpan Konfigurasi**.
3. Tekan **Aktifkan Akses Notifikasi**, lalu izinkan aplikasi ini di daftar.
4. Selesai. Setiap notifikasi pembayaran masuk otomatis diteruskan.

> HP harus tetap online agar notifikasi terpantau real-time.

## Build APK

### Opsi A — Otomatis via GitHub Actions (tanpa PC)
1. Buat repo GitHub baru, upload seluruh isi folder `androidapp/` ini.
2. Push ke branch `main`. Workflow `.github/workflows/build-apk.yml` akan
   otomatis mem-build APK.
3. Ambil APK di tab **Actions → artifact**, atau di **Releases** (tag `app-latest`).

### Opsi B — Android Studio
1. Buka folder ini di Android Studio.
2. Build > Build APK(s).
3. APK ada di `app/build/outputs/apk/debug/app-debug.apk`.

### Opsi C — CLI
```bash
gradle wrapper --gradle-version 8.4
./gradlew assembleDebug
```

## Catatan
- Aplикasi ini TIDAK menyentuh dana. Hanya membaca teks notifikasi dan
  mengirimnya ke server kamu sendiri.
- Nominal dicocokkan pakai kode unik, jadi tiap transaksi presisi.
