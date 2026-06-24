# Setup GitHub Secrets — Alhasanah Media

## Prasyarat

```bash
# Install GitHub CLI (Arch Linux)
sudo pacman -S github-cli

# Login ke GitHub
gh auth login

# Pastikan di repo yang benar
gh repo set-default
```

## Cara Cepat (Jalankan Script)

```bash
bash tools/init-github-secrets.sh
```

Script akan membaca `local.properties`, encode keystore, lalu menjalankan `gh secret set` untuk semua variabel.

## Cara Manual (Per Secret)

### 1. Keystore (Binary — pakai `--base64`)

```bash
gh secret set KEYSTORE_BASE64 < keystores.jks --base64
```

> `gh secret set` punya flag `--base64` otomatis, tidak perlu encode manual.

### 2. String Secrets (dari `local.properties`)

```bash
gh secret set RELEASE_KEYSTORE_PASSWORD --body "bssa1911"
gh secret set RELEASE_KEY_ALIAS --body "key1"
gh secret set RELEASE_KEY_PASSWORD --body "bssa1911"
gh secret set SUPABASE_URL --body "https://sldobkbolvrahlnowrga.supabase.co"
gh secret set SUPABASE_ANON_KEY --body "sb_publishable_T5chzlgY3hD8LbXBAVjOPg_cBswAKf_"
gh secret set AHMAD_SANUSI_API_KEY --body "ask_YstKaPqjWPZZcig7vdlFONUUYWC100WFj6Bne9zyQBE"
```

### 3. Google Services JSON (Multi-line — pakai file)

```bash
gh secret set GOOGLE_SERVICES_JSON < app/google-services.json
```

## Daftar Lengkap Secret

| Nama Secret | Tipe | Dari | Perintah |
|-------------|------|------|----------|
| `KEYSTORE_BASE64` | binary (base64) | `keystores.jks` | `gh secret set KEYSTORE_BASE64 < keystores.jks --base64` |
| `RELEASE_KEYSTORE_PASSWORD` | string | `local.properties` | `gh secret set RELEASE_KEYSTORE_PASSWORD --body "..."` |
| `RELEASE_KEY_ALIAS` | string | `local.properties` | `gh secret set RELEASE_KEY_ALIAS --body "..."` |
| `RELEASE_KEY_PASSWORD` | string | `local.properties` | `gh secret set RELEASE_KEY_PASSWORD --body "..."` |
| `SUPABASE_URL` | string | `local.properties` | `gh secret set SUPABASE_URL --body "..."` |
| `SUPABASE_ANON_KEY` | string | `local.properties` | `gh secret set SUPABASE_ANON_KEY --body "..."` |
| `AHMAD_SANUSI_API_KEY` | string | `local.properties` | `gh secret set AHMAD_SANUSI_API_KEY --body "..."` |
| `GOOGLE_SERVICES_JSON` | multiline | `app/google-services.json` | `gh secret set GOOGLE_SERVICES_JSON < app/google-services.json` |

## Verifikasi

```bash
gh secret list
```

## Cara Membuat Release

```bash
# Commit semua perubahan terbaru
git add . && git commit -m "deskripsi perubahan"

# Push ke main
git push origin main

# Buat tag untuk memicu workflow
git tag -a v1.0.0 -m "Rilis v1.0.0"
git push origin v1.0.0
```

Buka **GitHub repo → Actions** — workflow `Build & Release APK/AAB` akan berjalan otomatis.

## Catatan

- Semua secret ini **hanya untuk CI/CD** — jangan pernah dicomit ke repo.
- `local.properties` dan `keystores.jks` sudah di `.gitignore` — aman dari commit tidak sengaja.
- Jika ada perubahan nilai (misal ganti Supabase project), update secret dengan perintah yang sama.
- Nanti saat punya **Google Play Console**, tambah secret `PLAY_SERVICE_ACCOUNT_JSON` (isi file JSON service account).
