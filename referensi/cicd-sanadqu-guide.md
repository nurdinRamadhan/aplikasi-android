# CI/CD Setup Guide — sanadQu Multi-Pesantren

## Stack & Tools yang Dibutuhkan

| Tool | Fungsi | Install |
|---|---|---|
| **GitHub Actions** | CI/CD runner | Gratis via GitHub |
| **Supabase CLI** | Push migrasi database | `npm i -g supabase` |
| **Gradle** | Build Android | Sudah ada di project |
| **Fastlane** | Otomasi Play Store upload | `gem install fastlane` |
| **Ruby** | Dependency Fastlane | `brew install ruby` |
| **Bundler** | Manage Fastlane plugins | `gem install bundler` |

---

## Struktur Folder Target

```
sanadqu/
├── .github/
│   └── workflows/
│       ├── deploy-db.yml
│       └── deploy-android.yml
├── supabase/
│   └── migrations/
│       └── 00000000000000_initial_schema.sql   ← hasil db_dump
├── android/
│   ├── app/
│   │   ├── src/
│   │   │   ├── main/           ← kode shared
│   │   │   ├── alhasanah/      ← aset pesantren A
│   │   │   └── pesantrenB/     ← aset pesantren B
│   │   └── build.gradle.kts
│   └── fastlane/
│       ├── Appfile
│       └── Fastfile
└── README.md
```

---

## BAGIAN 1 — Persiapan Database (Supabase)

### Step 1: Install Supabase CLI

```bash
npm install -g supabase
supabase --version   # verifikasi
```

### Step 2: Login ke Supabase

```bash
supabase login
# Akan minta access token dari: https://app.supabase.com/account/tokens
```

### Step 3: Jadikan db_dump sebagai Migrasi Pertama

```bash
# Buat folder migrations jika belum ada
mkdir -p supabase/migrations

# Rename/copy file dump Anda
cp your_dump.sql supabase/migrations/00000000000000_initial_schema.sql
```

### Step 4: Link ke Project Supabase (per pesantren)

```bash
# Untuk pesantren A
supabase link --project-ref <project-ref-alhasanah>

# Untuk pesantren B (ganti ref-nya)
supabase link --project-ref <project-ref-pesantrenb>
```

Project ref ada di: `app.supabase.com/project/<ref>/settings/general`

### Step 5: Test Push Manual Dulu

```bash
supabase db push
# Pastikan tidak ada error sebelum diotomasi
```

---

## BAGIAN 2 — Persiapan Android (Product Flavors)

### Step 6: Setup Product Flavors di build.gradle.kts

```kotlin
// android/app/build.gradle.kts

android {
    flavorDimensions += "pesantren"

    productFlavors {
        create("alhasanah") {
            applicationId = "com.sanadqu.alhasanah"
            versionCode = 1
            versionName = "1.0.0"
            resValue("string", "app_name", "Al-Hasanah Digital")
            buildConfigField("String", "SUPABASE_URL",
                "\"${project.findProperty("SUPABASE_URL_ALHASANAH") ?: ""}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY",
                "\"${project.findProperty("SUPABASE_KEY_ALHASANAH") ?: ""}\"")
        }
        create("pesantrenB") {
            applicationId = "com.sanadqu.pesantrenb"
            versionCode = 1
            versionName = "1.0.0"
            resValue("string", "app_name", "Nama Pesantren B")
            buildConfigField("String", "SUPABASE_URL",
                "\"${project.findProperty("SUPABASE_URL_PESANTRENB") ?: ""}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY",
                "\"${project.findProperty("SUPABASE_KEY_PESANTRENB") ?: ""}\"")
        }
    }

    buildFeatures {
        buildConfig = true
    }
}
```

### Step 7: Buat Folder Aset per Flavor

```bash
# Pesantren A
mkdir -p android/app/src/alhasanah/res/drawable
mkdir -p android/app/src/alhasanah/res/values

# Pesantren B
mkdir -p android/app/src/pesantrenB/res/drawable
mkdir -p android/app/src/pesantrenB/res/values
```

Taruh logo masing-masing di folder `drawable/` dan override warna di `values/colors.xml`.

### Step 8: Gunakan BuildConfig di Kode Kotlin

```kotlin
// Contoh di SupabaseClient.kt
val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
)
```

### Step 9: Encode Keystore ke Base64

```bash
# Jalankan sekali di lokal
base64 -i your-keystore.jks | tr -d '\n' > keystore_base64.txt

# Isi file ini akan dimasukkan ke GitHub Secrets
# JANGAN commit file ini ke repo
```

---

## BAGIAN 3 — Setup Fastlane

### Step 10: Install Fastlane

```bash
cd android
gem install bundler fastlane

# Init Fastlane
fastlane init
```

### Step 11: Buat Appfile

```ruby
# android/fastlane/Appfile
json_key_file("fastlane/play-store-key.json")
package_name("com.sanadqu")   # base package
```

### Step 12: Buat Fastfile

```ruby
# android/fastlane/Fastfile

default_platform(:android)

platform :android do

  desc "Build dan upload ke Play Store"
  lane :deploy do |options|
    flavor     = options[:flavor]
    package    = options[:package]

    gradle(
      task: "bundle",
      flavor: flavor,
      build_type: "Release",
      properties: {
        "android.injected.signing.store.file"     => ENV["KEYSTORE_PATH"],
        "android.injected.signing.store.password" => ENV["KEYSTORE_PASSWORD"],
        "android.injected.signing.key.alias"      => ENV["KEY_ALIAS"],
        "android.injected.signing.key.password"   => ENV["KEY_PASSWORD"],
      }
    )

    upload_to_play_store(
      package_name: package,
      track: "production",
      aab: "app/build/outputs/bundle/#{flavor}Release/app-#{flavor}-release.aab",
      json_key: "fastlane/play-store-key.json",
      skip_upload_metadata: true,
      skip_upload_images: true,
      skip_upload_screenshots: true
    )
  end

end
```

### Step 13: Download Service Account Key dari Google Play

1. Buka [Google Play Console](https://play.google.com/console)
2. Setup > API Access > Create Service Account
3. Download file JSON
4. Simpan sebagai `android/fastlane/play-store-key.json`
5. **Jangan commit file ini** — tambahkan ke `.gitignore`

---

## BAGIAN 4 — GitHub Actions

### Step 14: Daftarkan Semua Secrets di GitHub

Buka: `GitHub Repo → Settings → Secrets and variables → Actions`

```
# Database
SUPABASE_ACCESS_TOKEN

# Per pesantren (duplikasi untuk setiap pesantren baru)
PROJECT_ID_ALHASANAH
DB_PASSWORD_ALHASANAH
SUPABASE_URL_ALHASANAH
SUPABASE_KEY_ALHASANAH

PROJECT_ID_PESANTRENB
DB_PASSWORD_PESANTRENB
SUPABASE_URL_PESANTRENB
SUPABASE_KEY_PESANTRENB

# Android (shared)
KEYSTORE_BASE64          ← isi dari keystore_base64.txt
KEY_ALIAS
KEYSTORE_PASSWORD
KEY_PASSWORD
PLAY_STORE_JSON          ← isi dari play-store-key.json (paste seluruh konten JSON)
```

### Step 15: Buat Workflow Database

```yaml
# .github/workflows/deploy-db.yml
name: Deploy Database Migrations

on:
  push:
    branches: [main]
    paths:
      - 'supabase/migrations/**'

jobs:
  deploy:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        include:
          - pesantren: alhasanah
            project_id_secret: PROJECT_ID_ALHASANAH
            db_password_secret: DB_PASSWORD_ALHASANAH
          - pesantren: pesantrenB
            project_id_secret: PROJECT_ID_PESANTRENB
            db_password_secret: DB_PASSWORD_PESANTRENB

    steps:
      - uses: actions/checkout@v4

      - uses: supabase/setup-cli@v1
        with:
          version: latest

      - name: Push Migrations ke ${{ matrix.pesantren }}
        run: |
          supabase link --project-ref ${{ secrets[matrix.project_id_secret] }}
          supabase db push --password ${{ secrets[matrix.db_password_secret] }}
        env:
          SUPABASE_ACCESS_TOKEN: ${{ secrets.SUPABASE_ACCESS_TOKEN }}
```

### Step 16: Buat Workflow Android

```yaml
# .github/workflows/deploy-android.yml
name: Deploy Android ke Play Store

on:
  push:
    branches: [main]
    paths:
      - 'android/**'

jobs:
  deploy:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        include:
          - flavor: alhasanah
            package: com.sanadqu.alhasanah
            supabase_url_secret: SUPABASE_URL_ALHASANAH
            supabase_key_secret: SUPABASE_KEY_ALHASANAH
          - flavor: pesantrenB
            package: com.sanadqu.pesantrenb
            supabase_url_secret: SUPABASE_URL_PESANTRENB
            supabase_key_secret: SUPABASE_KEY_PESANTRENB

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Ruby & Fastlane
        uses: ruby/setup-ruby@v1
        with:
          ruby-version: '3.2'
          bundler-cache: true
          working-directory: android

      - name: Decode Keystore
        run: |
          echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 --decode > android/keystore.jks

      - name: Setup Play Store Key
        run: |
          echo '${{ secrets.PLAY_STORE_JSON }}' > android/fastlane/play-store-key.json

      - name: Build & Deploy ${{ matrix.flavor }}
        working-directory: android
        run: bundle exec fastlane deploy flavor:${{ matrix.flavor }} package:${{ matrix.package }}
        env:
          KEYSTORE_PATH: keystore.jks
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
          SUPABASE_URL_ALHASANAH: ${{ secrets[matrix.supabase_url_secret] }}
          SUPABASE_KEY_ALHASANAH: ${{ secrets[matrix.supabase_key_secret] }}
```

---

## BAGIAN 5 — Tambah Pesantren Baru

Setiap ada pesantren baru, cukup lakukan ini:

### Di GitHub Secrets, tambahkan:
```
PROJECT_ID_<NAMA>
DB_PASSWORD_<NAMA>
SUPABASE_URL_<NAMA>
SUPABASE_KEY_<NAMA>
```

### Di deploy-db.yml, tambahkan 1 entry di matrix:
```yaml
- pesantren: namaPesantren
  project_id_secret: PROJECT_ID_NAMA
  db_password_secret: DB_PASSWORD_NAMA
```

### Di deploy-android.yml, tambahkan 1 entry di matrix:
```yaml
- flavor: namaPesantren
  package: com.sanadqu.namapesantren
  supabase_url_secret: SUPABASE_URL_NAMA
  supabase_key_secret: SUPABASE_KEY_NAMA
```

### Di build.gradle.kts, tambahkan 1 flavor:
```kotlin
create("namaPesantren") {
    applicationId = "com.sanadqu.namapesantren"
    resValue("string", "app_name", "Nama Pesantren")
    // ... BuildConfig fields
}
```

**Total effort tambah pesantren baru: ~15 menit.**

---

## Checklist Final

- [ ] Supabase CLI terinstall dan login
- [ ] db_dump menjadi `00000000000000_initial_schema.sql`
- [ ] Product Flavors terkonfigurasi di build.gradle.kts
- [ ] Folder aset per flavor sudah dibuat
- [ ] Keystore di-encode ke base64
- [ ] Play Store Service Account JSON sudah ada
- [ ] Semua secrets terdaftar di GitHub
- [ ] `deploy-db.yml` sudah ada di `.github/workflows/`
- [ ] `deploy-android.yml` sudah ada di `.github/workflows/`
- [ ] `.gitignore` sudah exclude: `keystore.jks`, `play-store-key.json`, `keystore_base64.txt`
- [ ] Push ke `main` dan verifikasi Actions berjalan

---

## File yang WAJIB Masuk .gitignore

```gitignore
# Secrets — jangan pernah commit
android/keystore.jks
android/keystore_base64.txt
android/fastlane/play-store-key.json
*.jks
.env
.env.local
```
