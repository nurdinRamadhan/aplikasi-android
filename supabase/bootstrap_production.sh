#!/bin/bash
# ==============================================================================
# Skrip Bootstrap Produksi - ALHASANAH MEDIA
# ==============================================================================
# Skrip ini digunakan untuk melakukan inisialisasi awal lingkungan produksi
# Supabase secara otomatis dan aman.
#
# CARA MENJALANKAN:
# 1. Pastikan Anda sudah login ke Supabase CLI: `supabase login`
# 2. Berikan izin eksekusi: `chmod +x bootstrap_production.sh`
# 3. Jalankan skrip:
#    ./bootstrap_production.sh "DATABASE_URL_PRODUKSI" "PROJECT_REF"
#
# PARAMETER:
# - DATABASE_URL_PRODUKSI : Connection string (Session mode) dari Dashboard Supabase.
# - PROJECT_REF           : Project ID (12 karakter) dari Dashboard Supabase.
# ==============================================================================

set -e # Keluar jika ada perintah yang gagal

if [ "$#" -ne 2 ]; then
    echo "Penggunaan: $0 <DATABASE_URL_PRODUKSI> <PROJECT_REF>"
    exit 1
fi

DB_URL=$1
PROJECT_REF=$2

# Validasi Dependensi
command -v psql >/dev/null 2>&1 || { echo >&2 "Error: psql tidak ditemukan. Instal postgresql-client."; exit 1; }
command -v supabase >/dev/null 2>&1 || { echo >&2 "Error: supabase CLI tidak ditemukan."; exit 1; }

echo "⚠️ PERINGATAN: Skrip ini akan mengaplikasikan skema ke database produksi: $PROJECT_REF"
read -p "Lanjutkan? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 1
fi

echo "--- Memulai Setup Produksi untuk Project: $PROJECT_REF ---"

# 1. Setup Skema & Ekstensi
echo "[1/4] Mendorong skema ke database..."
# Catatan: Ekstensi harus ada di file migrasi baseline (00000000000000_initial_schema.sql)
supabase db push --password "$DB_URL"
echo "Skema berhasil didorong."

# 2. Setup CLI Context
echo "[2/4] Menghubungkan CLI ke project produksi..."
supabase link --project-ref "$PROJECT_REF"

# 3. Sinkronisasi Histori Migrasi
echo "[3/4] Membersihkan histori migrasi lokal agar sinkron..."
supabase migration repair --status applied --all

# 4. Deploy Edge Functions
echo "[4/4] Deploying Edge Functions..."
supabase functions deploy

echo "--- Setup Produksi Selesai! ---"
echo "Langkah selanjutnya:"
echo "1. Verifikasi Checklist: ./supabase/PRODUCTION_READY_CHECKLIST.md"
echo "2. Konfigurasi semua Secrets di Dashboard Supabase."
