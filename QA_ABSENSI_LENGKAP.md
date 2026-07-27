# Q&A Absensi Lengkap — Siap Produksi

## Informasi Umum

### Q: Apa itu fitur Absensi Lengkap?
**A:** Fitur Absensi Lengkap adalah halaman di aplikasi Android (wali santri) yang menampilkan ringkasan kehadiran santri dari semua jenis kegiatan (Tahfidz, Mingguan, Ngaji, Sholat Hifdzi) dalam satu tampilan terpadu.

---

### Q: Siapa yang bisa mengakses fitur ini?
**A:** 
- **Wali Santri**: Hanya bisa melihat absensi santri sendiri (berdasarkan `niscustody_id`)
- **Staff Kehadiran** (super_admin, rois, dewan, kesantrian): Bisa melihat absensi semua santri

---

### Q: Bagaimana cara mengakses fitur ini?
**A:** 
1. Login sebagai wali santri
2. Buka drawer menu (geser dari kiri)
3. Klik "Absensi" (expandable submenu)
4. Pilih "Absensi Lengkap"

---

## Filter & Tampilan

### Q: Filter apa saja yang tersedia?
**A:** 
1. **Periode**: Hari Ini, Kemarin, 7 Hari, 30 Hari
2. **Jenis Absensi**: Semua, Tahfidz, Mingguan, Ngaji Kitab, Sholat Hifdzi
3. **Status Kehadiran**: Semua, Hadir, Izin, Sakit, Alpha, Sekolah, Pulang
4. **Mode Tampilan**: Harian, Semua Kegiatan

---

### Q: Apa perbedaan mode "Harian" dan "Semua Kegiatan"?
**A:** 
- **Harian**: Data dikelompokkan per hari dalam format accordion (expand/collapse). Setiap hari menampilkan ringkasan status kehadiran.
- **Semua Kegiatan**: Semua kegiatan ditampilkan dalam list flat (lurus) tanpa pengelompokan per hari.

---

### Q: Apa arti "Sekolah" dihitung sebagai "Izin"?
**A:** Untuk keperluan ringkasan, status **Sekolah** dihitung sebagai **Izin**. Ini karena santri yang sedang sekolah tidak hadir di kegiatan pesantren, sehingga dianggap izin.

---

### Q: Bagaimana cara mengubah periode waktu?
**A:** 
1. Klik tombol filter (ikon filter) di pojok kanan atas
2. Pilih periode: Hari Ini / Kemarin / 7 Hari / 30 Hari
3. Klik "Terapkan Filter"

---

## Kegiatan & Sesi

### Q: Jenis kegiatan apa saja yang ditampilkan?
**A:** 
| No | Jenis | Contoh Kegiatan |
|----|-------|-----------------|
| 1 | Tahfidz | Ziyadah (Hafalan Baru), Murojaah (Ulang Hafalan) |
| 2 | Mingguan | Hafalan, Istighosah, Ngaos Aang, Tilawah, Tawasul, MHQ, Muhadhoroh |
| 3 | Ngaji | Ngaji Kitab |
| 4 | Sholat Hifdzi | Sholat Hifdzi |

---

### Q: Apa itu sesi "Pagi" dan "Siang"?
**A:** 
- **Pagi**: Khusus untuk kegiatan **Ziyadah** (Hafalan Baru)
- **Siang**: Khusus untuk kegiatan **Murojaah** (Ulang Hafalan)
- Kegiatan lain (Mingguan, Ngaji, Sholat Hifdzi) **tidak memiliki label sesi**

---

### Q: Mengapa hanya Ziyadah dan Murojaah yang memiliki sesi?
**A:** Karena kegiatan Tahfidz (Ziyadah dan Murojaah) dijadwalkan pada waktu-waktu tertentu:
- Ziyadah biasanya dilakukan pada **pagi hari**
- Murojaah biasanya dilakukan pada **siang hari**

Sedangkan kegiatan lain tidak memiliki jadwal waktu spesifik yang perlu ditampilkan.

---

## Status Kehadiran

### Q: Status kehadiran apa saja yang ada?
**A:** 
| Status | Warna | Keterangan |
|--------|-------|------------|
| **Hadir** | Hijau (#16A34A) | Santri hadir sesuai jadwal |
| **Izin** | Kuning (#F59E0B) | Santri izin / ada keterangan (termasuk Sekolah) |
| **Sakit** | Merah (#EF4444) | Santri sakit / tidak sehat |
| **Alpha** | Abu-abu (#6B7280) | Santri tidak hadir tanpa keterangan |
| **Pulang** | Biru (#0891B2) | Santri pulang |

---

### Q: Apa perbedaan "Alpha" dan "GHAIB"?
**A:** 
- **Alpha**: Status standar untuk tidak hadir tanpa keterangan
- **GHAIB**: Nama lain untuk Alpha di beberapa subsistem (mingguan, ngaji, sholat_hifdzi)

Di aplikasi, keduanya ditampilkan sebagai **Alpha**.

---

## Ringkasan & Grafik

### Q: Apa saja yang ditampilkan di bagian Ringkasan?
**A:** 
1. **Profil Santri**: Avatar, Nama, Kelas, NIS, Persentase Kehadiran
2. **Tanggal**: Rentang waktu yang dipilih
3. **Ringkasan Kehadiran**: 
   - Hadir (jumlah + persentase)
   - Izin (jumlah + persentase, termasuk Sekolah)
   - Sakit (jumlah + persentase)
   - Alpha (jumlah + persentase)
   - Pulang (jumlah + persentase, jika ada)
4. **Grafik Kehadiran**: Chart bar harian dengan warna status
5. **Daftar Aktivitas**: Accordion per hari atau flat list

---

### Q: Bagaimana cara membaca grafik kehadiran?
**A:** 
- **Sumbu X**: Tanggal (28 Jul, 29 Jul, dst.)
- **Sumbu Y**: Jumlah kegiatan (0, 1, 2, 3, dst.)
- **Warna bar**: 
  - Hijau = Hadir
  - Kuning = Izin
  - Merah = Sakit
  - Abu-abu = Alpha
- **Badge di atas bar**: Persentase kehadiran hari tersebut

---

### Q: Bagaimana cara membaca ringkasan per hari di mode Harian?
**A:** 
Contoh:
```
Selasa, 29 Juli 2026          67%
● 2  ○ 1  ● 0  ● 1
```
- **67%**: Persentase kehadiran hari tersebut
- **● 2**: 2 kegiatan Hadir (hijau)
- **○ 1**: 1 kegiatan Izin (kuning)
- **● 0**: 0 kegiatan Sakit (merah)
- **● 1**: 1 kegiatan Alpha (abu-abu)

Klik untuk expand/collapse melihat detail kegiatan.

---

## Teknis

### Q: Berapa batas maksimal hari yang bisa dipilih?
**A:** Maksimal **31 hari** per request. Jika memilih lebih dari 31 hari, akan muncul error.

---

### Q: Bagaimana jika data absensi kosong?
**A:** Akan ditampilkan pesan "Tidak ada data kegiatan untuk periode ini" dengan ilustrasi kosong.

---

### Q: Bagaimana cara refresh data?
**A:** 
1. Tarik ke bawah (pull to refresh), atau
2. Klik tombol refresh di pojok kanan atas

---

### Q: Apakah fitur ini mendukung dark mode?
**A:** Ya, fitur ini mendukung dark mode. Semua komponen menggunakan `MaterialTheme.colorScheme` yang otomatis beradaptasi.

---

## Troubleshooting

### Q: Data absensi tidak muncul?
**A:** 
1. Pastikan periode waktu sudah benar
2. Pastikan filter "Jenis Absensi" dan "Status" tidak memblokir data
3. Coba refresh data
4. Jika masih bermasalah, hubungi admin

---

### Q: Persentase kehadiran tidak sesuai?
**A:** 
- Persentase dihitung: `(Jumlah Hadir / Total Kegiatan) × 100%`
- Status **Sekolah** dihitung sebagai **Izin** di ringkasan
- Status **Pulang** dihitung terpisah

---

### Q: Mengapa有些 kegiatan tidak memiliki label sesi?
**A:** Hanya kegiatan **Tahfidz** (Ziyadah dan Murojaah) yang memiliki label sesi (Pagi/Siang). Kegiatan lain (Mingguan, Ngaji, Sholat Hifdzi) tidak memiliki label sesi karena tidak dijadwalkan pada waktu spesifik.

---

## Limitasi

### Q: Apa saja limitasi fitur ini?
**A:** 
1. Maksimal 31 hari per request
2. Hanya menampilkan data kehadiran, bukan data hafalan
3. Tidak bisa mengubah status kehadiran (hanya melihat)
4. Tidak ada notifikasi untuk data baru

---

### Q: Apakah fitur ini tersedia untuk semua santri?
**A:** Ya, semua santri yang memiliki NIS (Nomor Induk Santri) dapat dilihat absensinya oleh wali yang sah.

---

## Update Terakhir

**Versi**: 2.0 (27 Juli 2026)

**Perubahan**:
1. ✅ Filter Periode (Hari Ini, Kemarin, 7 Hari, 30 Hari)
2. ✅ Filter Jenis Absensi (Tahfidz, Mingguan, Ngaji, Sholat Hifdzi)
3. ✅ Filter Status Kehadiran (Hadir, Izin, Sakit, Alpha, Sekolah, Pulang)
4. ✅ Mode Tampilan (Harian, Semua Kegiatan)
5. ✅ Accordion per hari dengan expand/collapse
6. ✅ Sesi Pagi (Ziyadah) dan Siang (Murojaah)
7. ✅ Sekolah dihitung sebagai Izin di ringkasan
8. ✅ Bottom sheet filter modern
