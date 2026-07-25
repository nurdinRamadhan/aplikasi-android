-- ============================================================
-- Data dummy 1 minggu untuk Ahmad (20262123)
-- 4-8 Juli 2026 | Ngaji PAGI + Murojaah SIANG + Mingguan
-- ============================================================

-- 1) Tahfidz Sesi: Murojaah SIANG (Jul 4, 6, 7, 8)
INSERT INTO public.tahfidz_sesi (id, kegiatan_id, tanggal, sesi, status, created_by)
VALUES
  (gen_random_uuid(), 'MUROJAAH', '2026-07-04', 'SIANG', 'OPEN', '86f0628b-808d-4562-ad25-478987d5211d'),
  (gen_random_uuid(), 'MUROJAAH', '2026-07-06', 'SIANG', 'OPEN', '86f0628b-808d-4562-ad25-478987d5211d'),
  (gen_random_uuid(), 'MUROJAAH', '2026-07-07', 'SIANG', 'OPEN', '86f0628b-808d-4562-ad25-478987d5211d'),
  (gen_random_uuid(), 'MUROJAAH', '2026-07-08', 'SIANG', 'OPEN', '86f0628b-808d-4562-ad25-478987d5211d');

-- 2) Mingguan Sesi
--    minggu_ke=4 (Jul 7) sudah ada: NGAOS_AANG, TILAWAH, TAWASUL, MHQ
--    Tambah: HAFALAN & ISTIGHOSAH untuk Jul 6, MUHADHOROH untuk Jul 8
INSERT INTO public.mingguan_sesi (id, kegiatan_id, bulan_hijriah, tahun_hijriah, bulan_hijriah_number, minggu_ke, tanggal, status, created_by)
VALUES
  (gen_random_uuid(), 'HAFALAN',    'Muharram', 1448, 1, 4, '2026-07-06', 'OPEN', '86f0628b-808d-4562-ad25-478987d5211d'),
  (gen_random_uuid(), 'ISTIGHOSAH', 'Muharram', 1448, 1, 4, '2026-07-08', 'OPEN', '86f0628b-808d-4562-ad25-478987d5211d'),
  (gen_random_uuid(), 'MUHADHOROH', 'Muharram', 1448, 1, 4, '2026-07-06', 'OPEN', '86f0628b-808d-4562-ad25-478987d5211d');

-- 3) Ngaji Absensi (Jul 6=HADIR, Jul 7=SAKIT, Jul 8=HADIR)
INSERT INTO public.ngaji_absensi (tahun_hijriah, bulan_hijriah_number, hari_hijriah, sesi_ke, santri_nis, status)
VALUES
  (1448, 1, 21, 1, '20262123', 'HADIR'),
  (1448, 1, 22, 1, '20262123', 'SAKIT'),
  (1448, 1, 23, 1, '20262123', 'HADIR')
ON CONFLICT (tahun_hijriah, bulan_hijriah_number, hari_hijriah, sesi_ke, santri_nis) DO NOTHING;

-- 4) Tahfidz Absensi: Murojaah SIANG
--    Jul 4=SEKOLAH, Jul 6=HADIR, Jul 7=GHAIB, Jul 8=HADIR
INSERT INTO public.tahfidz_absensi (sesi_id, santri_nis, status)
SELECT s.id, '20262123', v.status
FROM public.tahfidz_sesi s
JOIN (VALUES
  ('2026-07-04', 'SEKOLAH'),
  ('2026-07-06', 'HADIR'),
  ('2026-07-07', 'GHAIB'),
  ('2026-07-08', 'HADIR')
) AS v(tanggal, status) ON s.tanggal = v.tanggal::date AND s.sesi = 'SIANG' AND s.kegiatan_id = 'MUROJAAH'
ON CONFLICT (sesi_id, santri_nis) DO NOTHING;

-- 5) Mingguan Absensi
--    Jul 6: HAFALAN=HADIR, MUHADHOROH=GHAIB
--    Jul 7: sudah ada NGAOS=HADIR, TILAWAH=GHAIB
--    Jul 8: ISTIGHOSAH=HADIR
INSERT INTO public.mingguan_absensi (sesi_id, santri_nis, status)
SELECT s.id, '20262123', v.status
FROM public.mingguan_sesi s
JOIN (VALUES
  ('HAFALAN',    '2026-07-06', 'HADIR'),
  ('MUHADHOROH', '2026-07-06', 'GHAIB'),
  ('ISTIGHOSAH', '2026-07-08', 'HADIR')
) AS v(kegiatan_id, tanggal, status) ON s.kegiatan_id = v.kegiatan_id AND s.tanggal = v.tanggal::date
ON CONFLICT (sesi_id, santri_nis) DO NOTHING;
