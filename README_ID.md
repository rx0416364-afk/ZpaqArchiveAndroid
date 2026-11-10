# Zpaq Archive Android

Aplikasi Android minimalis untuk membuat dan mengekstrak arsip **ZPAQ (.zpaq)** menggunakan engine ZPAQ 7.15.

## Fitur

- Membuat arsip `.zpaq` dari beberapa file sekaligus.
- Membuat arsip dari satu folder beserta struktur subfolder-nya.
- Mengekstrak arsip ke folder pilihan melalui Android Storage Access Framework.
- Tingkat kompresi ZPAQ 0–5; level 1 menjadi pilihan awal yang seimbang.
- Pemrosesan lokal di perangkat; tidak mengunggah file ke server.
- Antarmuka Bahasa Indonesia, ringkas, dan tidak memerlukan izin akses seluruh penyimpanan.

## Format dan kompatibilitas

- Format arsip: **ZPAQ level 2 / `.zpaq`**, memakai implementasi upstream ZPAQ 7.15.
- Kompatibilitas: perangkat Android API 23 (Android 6.0) atau lebih baru.
- ABI yang disiapkan: `arm64-v8a`, `armeabi-v7a`, dan `x86_64`.
- Native engine dibangun dengan `-DNOJIT` agar tidak bergantung pada JIT x86/SSE2; hal ini penting untuk perangkat ARM Android.
- Android tidak menyediakan pembaca `.zpaq` bawaan. Arsip perlu dibuka melalui aplikasi Zpaq Archive atau program lain yang mendukung format ZPAQ.

## Build APK melalui GitHub Actions

1. Buat repository GitHub baru.
2. Unggah seluruh isi folder proyek ini ke repository (file `settings.gradle.kts`, folder `app`, dan folder `.github` harus berada di root repository).
3. Buka tab **Actions** pada repository.
4. Pilih workflow **Build Android APK**, lalu jalankan **Run workflow**. Workflow juga berjalan otomatis saat ada push ke branch `main` atau `master`.
5. Setelah selesai, buka hasil run dan unduh artifact **ZpaqArchive-debug-apk**. Di dalamnya terdapat `app-debug.apk`.

## Build lokal

Persyaratan: JDK 17, Android SDK Platform 35, Android NDK `27.2.12479018`, CMake `3.22.1`, dan Gradle 8.9.

```bash
gradle assembleDebug
```

APK debug dibuat di `app/build/outputs/apk/debug/app-debug.apk`.

## Catatan penggunaan

- Saat membuat arsip, pilih file dan/atau folder, tentukan nama `.zpaq`, pilih level kompresi, lalu simpan lokasi output di pemilih dokumen Android.
- Saat mengekstrak, pilih arsip dan folder tujuan. Struktur folder di dalam arsip akan dipertahankan saat diekspor.
- Level kompresi yang lebih tinggi dapat memerlukan waktu dan memori lebih banyak. Pada HP dengan RAM terbatas, mulai dari level 1 atau 2.
- Buat arsip dari file yang kamu percaya dan pastikan tersedia ruang kosong yang cukup untuk proses sementara serta hasil ekstraksi.

## Sumber dan lisensi

Engine ZPAQ 7.15 karya Matt Mahoney berada di direktori `app/src/main/cpp/`. Berkas `zpaq.cpp`, `libzpaq.cpp`, `libzpaq.h`, dan `COPYING` memuat engine serta pemberitahuan lisensinya. Kode Android pada proyek ini merupakan lapisan antarmuka dan integrasi JNI.
