#!/usr/bin/env bash
# init-github-secrets.sh
# Baca local.properties + keystores.jks, lalu set semua secret ke GitHub repo aktif.
# Jalankan: bash tools/init-github-secrets.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$SCRIPT_DIR"

LOCAL_PROPERTIES="$SCRIPT_DIR/local.properties"
KEYSTORE="$SCRIPT_DIR/keystores.jks"
GOOGLE_SERVICES="$SCRIPT_DIR/app/google-services.json"

echo "=== Setup GitHub Secrets untuk Alhasanah Media ==="

if [ ! -f "$LOCAL_PROPERTIES" ]; then
  echo "ERROR: $LOCAL_PROPERTIES tidak ditemukan!"
  exit 1
fi

if [ ! -f "$KEYSTORE" ]; then
  echo "ERROR: $KEYSTORE tidak ditemukan!"
  exit 1
fi

if [ ! -f "$GOOGLE_SERVICES" ]; then
  echo "ERROR: $GOOGLE_SERVICES tidak ditemukan!"
  exit 1
fi

# Cek gh CLI
if ! command -v gh &> /dev/null; then
  echo "ERROR: GitHub CLI (gh) tidak terinstall!"
  echo "Install: sudo pacman -S github-cli"
  exit 1
fi

# Cek login
gh auth status &> /dev/null || { echo "ERROR: Belum login! Jalankan: gh auth login"; exit 1; }

# Parse local.properties (handle dots in keys)
RELEASE_KEYSTORE_PASSWORD=$(grep '^release.keystore.password=' "$LOCAL_PROPERTIES" | cut -d'=' -f2-)
RELEASE_KEY_ALIAS=$(grep '^release.key.alias=' "$LOCAL_PROPERTIES" | cut -d'=' -f2-)
RELEASE_KEY_PASSWORD=$(grep '^release.key.password=' "$LOCAL_PROPERTIES" | cut -d'=' -f2-)
SUPABASE_URL=$(grep '^supabase.url=' "$LOCAL_PROPERTIES" | cut -d'=' -f2-)
SUPABASE_ANON_KEY=$(grep '^supabase.anon.key=' "$LOCAL_PROPERTIES" | cut -d'=' -f2-)
AHMAD_SANUSI_API_KEY=$(grep '^ahmadsanusi.api.key=' "$LOCAL_PROPERTIES" | cut -d'=' -f2-)

echo ""
echo "1. KEYSTORE_BASE64 — dari keystores.jks"
base64 -w0 "$KEYSTORE" | gh secret set KEYSTORE_BASE64 -b "@-"
echo "   OK"

echo ""
echo "2. String secrets — dari local.properties"
gh secret set RELEASE_KEYSTORE_PASSWORD -b "$RELEASE_KEYSTORE_PASSWORD"
gh secret set RELEASE_KEY_ALIAS -b "$RELEASE_KEY_ALIAS"
gh secret set RELEASE_KEY_PASSWORD -b "$RELEASE_KEY_PASSWORD"
gh secret set SUPABASE_URL -b "$SUPABASE_URL"
gh secret set SUPABASE_ANON_KEY -b "$SUPABASE_ANON_KEY"
gh secret set AHMAD_SANUSI_API_KEY -b "$AHMAD_SANUSI_API_KEY"
echo "   OK"

echo ""
echo "3. GOOGLE_SERVICES_JSON — dari app/google-services.json"
gh secret set GOOGLE_SERVICES_JSON -b "$(cat "$GOOGLE_SERVICES")"
echo "   OK"

echo ""
echo "=== SEMUA SECRET BERHASIL DISET ==="
echo ""
echo "Verifikasi:"
gh secret list
