# Panduan Release — Alhasanah Media Android

## Daftar Isi

1. [Cara Membuat Release Baru](#1-cara-membuat-release-baru)
2. [Skema Versi](#2-skema-versi)
3. [GitHub Secrets](#3-github-secrets)
4. [Konfigurasi Workflow](#4-konfigurasi-workflow)
5. [Update Library / Dependency](#5-update-library--dependency)
6. [Self-Update / In-App Update](#6-self-update--in-app-update)
7. [Migrasi ke Google Play Store](#7-migrasi-ke-google-play-store)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. Cara Membuat Release Baru

### Prasyarat

- Semua perubahan sudah di-commit ke branch `main`
- Build lokal sukses: `./gradlew assembleDebug`
- Sudah login GitHub CLI: `gh auth login`

### Langkah Release

```bash
# 1. Commit semua perubahan
git add .
git commit -m "deskripsi perubahan yang jelas"

# 2. Push ke main
git push origin main

# 3. Buat tag versi baru (ikut skema di bawah)
git tag -a v1.1.0 -m "Rilis v1.1.0"

# 4. Push tag — workflow otomatis jalan
git push origin v1.1.0
```

### Apa yang Terjadi?

Push tag `v*` akan memicu GitHub Actions workflow `.github/workflows/build-release.yml`:

1. **Decode keystore** — baca `KEYSTORE_BASE64` dari secrets → decode jadi file `keystores.jks`
2. **Create local.properties** — isi dari secrets (pasword keystore, Supabase, dll)
3. **Decode google-services.json** — dari secrets
4. **Ekstrak versi** — tag `v1.1.0` → versionName=`1.1.0`, versionCode=`10100`
5. **Build** — `bundleRelease` (AAB) + `assembleRelease` (APK) dengan signing release
6. **Upload** — ke GitHub Release sebagai attachment

Hasil akhir: https://github.com/nurdinRamadhan/aplikasi-android/releases/tag/v1.1.0

---

## 2. Skema Versi

Format tag: `vMAJOR.MINOR.PATCH` (contoh: `v1.0.0`, `v1.1.0`, `v2.0.0`)

| Tag | versionName | versionCode | Keterangan |
|-----|-------------|-------------|------------|
| `v1.0.0` | 1.0.0 | 10000 | Rilis pertama |
| `v1.0.1` | 1.0.1 | 10001 | Bug fix minor |
| `v1.1.0` | 1.1.0 | 10100 | Fitur baru |
| `v2.0.0` | 2.0.0 | 20000 | Perubahan besar |

Aturan:
- **MAJOR** — perubahan besar/breaking
- **MINOR** — fitur baru (tidak breaking)
- **PATCH** — bug fix, UI tweak

`versionCode` dihitung otomatis dari tag: `MAJOR * 10000 + MINOR * 100 + PATCH`

---

## 3. GitHub Secrets

Semua secret sudah diset via `tools/init-github-secrets.sh`. Berikut daftarnya:

| Secret | Isi | Diperbarui Kembali Jika... |
|--------|-----|---------------------------|
| `KEYSTORE_BASE64` | `base64 keystores.jks` | Ganti keystore |
| `RELEASE_KEYSTORE_PASSWORD` | Password keystore | Ganti password |
| `RELEASE_KEY_ALIAS` | `key1` | Ganti alias |
| `RELEASE_KEY_PASSWORD` | Password key | Ganti password |
| `SUPABASE_URL` | URL Supabase project | Ganti project |
| `SUPABASE_ANON_KEY` | Anon key Supabase | Ganti project |
| `AHMAD_SANUSI_API_KEY` | API key | Di-refresh |
| `GOOGLE_SERVICES_JSON` | Isi file JSON Firebase | Update Firebase |

### Cara update secret manual

```bash
# String biasa
gh secret set NAMA_SECRET -b "nilai_baru"

# File multi-line (json, dll)
gh secret set NAMA_SECRET -b "$(cat file.json)"

# Binary base64 (keystore)
base64 -w0 keystores.jks | gh secret set KEYSTORE_BASE64 -b "@-"
```

### Cara cepat (semua sekaligus)

```bash
bash tools/init-github-secrets.sh
```

---

## 4. Konfigurasi Workflow

File: `.github/workflows/build-release.yml`

Trigger: push tag `v*`

### Struktur

```yaml
name: Build & Release APK/AAB

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: write        # <-- WAJIB untuk create release
    steps:
      # ... (checkout, setup Java, Gradle)
      # ... (decode secrets, build)
      - name: Create Release
        uses: softprops/action-gh-release@v2
```

### Catatan Penting

- **`permissions: contents: write`** — harus ada, tanpanya GITHUB_TOKEN tidak bisa create release
- **Gradle property** versi dilewatkan via `-PversionCode=... -PversionName=...` (override di `build.gradle.kts`)
- **Base64 keystore** disimpan via `gh secret set KEYSTORE_BASE64 -b "$(base64 -w0 keystores.jks)"`

---

## 5. Update Library / Dependency

### Cara Update

1. Buka `gradle/libs.versions.toml`
2. Update versi library yang diinginkan
3. Sync project → `./gradlew assembleDebug` untuk verifikasi
4. Jika ada deprecation/breaking change, sesuaikan kode

### Library Utama Saat Ini

| Library | Versi | File |
|---------|-------|------|
| AGP (Android Gradle Plugin) | 8.7.3 | `gradle/libs.versions.toml` |
| Kotlin | 2.1.0 | `gradle/libs.versions.toml` |
| Compose BOM | 2025.02.00 | `gradle/libs.versions.toml` |
| Supabase Kotlin | 3.1.1 | `gradle/libs.versions.toml` |
| Firebase BOM | 34.12.0 | `gradle/libs.versions.toml` |
| Gradle Wrapper | 8.13 | `gradle/wrapper/gradle-wrapper.properties` |

### Update Gradle Wrapper

```bash
./gradlew wrapper --gradle-version 8.14
```

---

## 6. Self-Update / In-App Update

### Kondisi Saat Ini

**Belum ada.** Workflow hanya build + upload ke GitHub Release. Aplikasi tidak otomatis mengecek update.

### Cara Kerja Self-Update

Untuk mengecek update dari GitHub Releases, aplikasi perlu:

1. **Cek versi** — GET `https://api.github.com/repos/nurdinRamadhan/aplikasi-android/releases/latest`
2. **Bandingkan** — `versionCode` lokal vs release terbaru
3. **Download APK** — dari asset release (URL: `https://github.com/.../releases/download/v1.0.4/AlhasanahMedia-v1.0.4.apk`)
4. **Install** — Intent `ACTION_VIEW` dengan `content://` URI + `REQUEST_INSTALL_PACKAGES` permission

### Library yang Bisa Dipakai

| Library | Kelebihan | Kekurangan |
|---------|-----------|------------|
| **Google Play In-App Updates** | Resmi, otomatis, user-friendly | **Hanya untuk Play Store** |
| **Custom GitHub checker** | Bebas, langsung dari release | Perlu handle permission & download sendiri |
| **AppUpdater** (by rames3) | Siap pakai, support GitHub | Sudah lama tidak update |

### Rekomendasi

Gunakan **Google Play In-App Updates** jika sudah punya Play Console. Kalau masih pakai GitHub Releases, buat implementasi sederhana sendiri dengan **Ktor Client** (sudah ada di project) untuk fetch release terbaru.

---

## 7. Migrasi ke Google Play Store

### Syarat

- Akun Google Play Developer ($25)
- Aplikasi terdaftar di Play Console
- **Service Account** Google Play + JSON key

### Langkah

1. Buat service account → download JSON key → simpan sebagai secret `PLAY_SERVICE_ACCOUNT_JSON`
2. Tambah step di workflow setelah `Rename AAB`:

```yaml
- name: Upload to Play Store (Internal)
  uses: r0adkll/upload-google-play@v1
  with:
    serviceAccountJsonPlainText: ${{ secrets.PLAY_SERVICE_ACCOUNT_JSON }}
    packageName: com.alhasanah.alhasanahmedia
    releaseFiles: app/build/outputs/bundle/release/AlhasanahMedia-v${{ env.VERSION_NAME }}.aab
    track: internal
```

3. AAB sudah siap dari langkah `bundleRelease` — tinggal upload.

---

## 8. Troubleshooting

### Build gagal: `base64: invalid input`

**Masalah:** Secret `KEYSTORE_BASE64` rusak/truncated karena cara set sebelumnya.
**Solusi:** Set ulang via file:

```bash
base64 -w0 keystores.jks > /tmp/keystore.b64
gh secret set KEYSTORE_BASE64 -b "$(cat /tmp/keystore.b64)"
```

### Release gagal: `Resource not accessible by integration` (403)

**Masalah:** GITHUB_TOKEN tidak punya izin `contents: write`.
**Solusi:** Tambah `permissions: contents: write` di workflow YAML (sudah ditambahkan).

### Push tag tidak trigger workflow

**Masalah:** Filter `tags: - 'v*'` tidak cocok.
**Solusi:** Pastikan tag diawali `v` besar. Contoh: `v1.0.0` (bukan `1.0.0`).

### Build gagal karena library version mismatch

**Masalah:** Ada perubahan breaking di library.
**Solusi:** Cek log Gradle, cari tahu library mana yang error, update kode sesuai migrasi guide library tersebut.

---

## Referensi

- Workflow file: `.github/workflows/build-release.yml`
- Script setup: `tools/init-github-secrets.sh`
- Panduan secrets: `SETUP_GITHUB_SECRETS.md`
- Konfigurasi build: `app/build.gradle.kts`
- Repo GitHub: https://github.com/nurdinRamadhan/aplikasi-android
