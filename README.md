# Zpaq Archive Android

Aplikasi Android untuk membuat dan mengekstrak arsip ZPAQ (`.zpaq`) secara lokal di perangkat.

## Fitur

- Membuat arsip dari beberapa file atau satu folder beserta subfolder.
- Mengekstrak arsip ke folder pilihan melalui Android Storage Access Framework.
- Memilih level kompresi ZPAQ 0–5.
- Tidak mengunggah file pengguna ke server.
- Mendukung Android 6.0 (API 23) atau lebih baru.

## Unduh APK

Build debug terbaru tersedia sebagai artifact **ZpaqArchive-debug-apk** di [GitHub Actions](https://github.com/rx0416364-afk/ZpaqArchiveAndroid/actions). Buka run yang sukses, lalu unduh artifact dari bagian **Artifacts**. APK di dalamnya bernama `app-debug.apk`.

[Run build yang berhasil](https://github.com/rx0416364-afk/ZpaqArchiveAndroid/actions/runs/42812447813)

## Build

Workflow otomatis berjalan saat ada push ke `main` atau `master`. Untuk menjalankan manual, buka **Actions → Build Android APK → Run workflow**.

Workflow menggunakan JDK 17, Gradle 8.9, Android SDK Platform 35, NDK 27.2.12479018, dan CMake 3.22.1.

Build lokal memerlukan Gradle 8.9 dan Android SDK/NDK tersebut:

```bash
gradle --no-daemon assembleDebug
```

APK dibuat di `app/build/outputs/apk/debug/app-debug.apk`.

## Struktur

- `app/src/main/java/`: antarmuka Android dan pemanggilan JNI.
- `app/src/main/cpp/`: JNI bridge dan engine ZPAQ.
- `.github/workflows/android.yml`: build APK dan unggah artifact.

## Lisensi

Kode aplikasi dan integrasi Android asli dilisensikan di bawah MIT; lihat [LICENSE](LICENSE). Engine ZPAQ dan komponen pihak ketiga membawa lisensinya sendiri; pemberitahuan disimpan di [app/src/main/cpp/COPYING](app/src/main/cpp/COPYING) dan header source terkait. Jangan menganggap lisensi MIT root menggantikan pemberitahuan tersebut.

Dokumentasi tambahan berbahasa Indonesia ada di [README_ID.md](README_ID.md).
