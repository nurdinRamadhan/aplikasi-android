CREATE OR REPLACE FUNCTION public.get_ringkasan_absensi_mingguan(p_santri_nis text, p_start_date date)
RETURNS json
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO 'public'
AS $function$
DECLARE
  v_end_date date;
  v_santri_nama text;
  v_result json;
BEGIN
  IF auth.uid() IS NULL OR NOT public.can_view_attendance_of_santri(p_santri_nis) THEN
    RAISE EXCEPTION 'Akses ditolak untuk ringkasan absensi santri ini.' USING ERRCODE = '42501';
  END IF;

  v_end_date := p_start_date + 5;

  SELECT nama INTO v_santri_nama
  FROM public.santri
  WHERE nis = p_santri_nis;

  WITH all_absensi AS (
    SELECT
      s.tanggal,
      COALESCE(k.label, s.kegiatan_id) AS kegiatan,
      COALESCE(s.sesi, '-') AS sesi,
      a.status
    FROM public.tahfidz_absensi a
    JOIN public.tahfidz_sesi s ON s.id = a.sesi_id
    LEFT JOIN public.kegiatan_tahfidz k ON k.id = s.kegiatan_id
    WHERE a.santri_nis = p_santri_nis
      AND s.tanggal BETWEEN p_start_date AND v_end_date

    UNION ALL

    SELECT
      s.tanggal,
      COALESCE(k.label, s.kegiatan_id) AS kegiatan,
      '-' AS sesi,
      a.status
    FROM public.mingguan_absensi a
    JOIN public.mingguan_sesi s ON s.id = a.sesi_id
    LEFT JOIN public.mingguan_kegiatan k ON k.id = s.kegiatan_id
    WHERE a.santri_nis = p_santri_nis
      AND s.tanggal BETWEEN p_start_date AND v_end_date

    UNION ALL

    SELECT
      s.tanggal,
      COALESCE(k.label, 'Ngaji') AS kegiatan,
      'Sesi ' || a.sesi_ke AS sesi,
      a.status
    FROM public.ngaji_absensi a
    JOIN public.ngaji_sesi s ON s.kegiatan_id = 'NGAJI'
      AND s.tahun_hijriah = a.tahun_hijriah
      AND s.bulan_hijriah_number = a.bulan_hijriah_number
      AND s.hari_ke = a.hari_hijriah
      AND s.sesi_ke = a.sesi_ke
    LEFT JOIN public.ngaji_kegiatan k ON k.id = s.kegiatan_id
    WHERE a.santri_nis = p_santri_nis
      AND s.tanggal BETWEEN p_start_date AND v_end_date

    UNION ALL

    SELECT
      s.tanggal,
      COALESCE(k.label, s.kegiatan_id) AS kegiatan,
      '-' AS sesi,
      a.status
    FROM public.sholat_hifdzi_absensi a
    JOIN public.sholat_hifdzi_sesi s ON s.id = a.sesi_id
    LEFT JOIN public.sholat_hifdzi_kegiatan k ON k.id = s.kegiatan_id
    WHERE a.santri_nis = p_santri_nis
      AND s.tanggal BETWEEN p_start_date AND v_end_date
  )
  SELECT json_agg(
    json_build_object(
      'hari', (ARRAY['Minggu','Senin','Selasa','Rabu','Kamis','Jumat','Sabtu'])[EXTRACT(DOW FROM tanggal)::int + 1],
      'tanggal', to_char(tanggal, 'YYYY-MM-DD'),
      'kegiatan', kegiatan,
      'sesi', sesi,
      'status', CASE
        WHEN upper(status) = 'HADIR' THEN 'HADIR'
        WHEN upper(status) IN ('GHAIB', 'ALFA', 'ALPHA') THEN 'ALPHA'
        WHEN upper(status) = 'SAKIT' THEN 'SAKIT'
        WHEN upper(status) = 'IZIN' THEN 'IZIN'
        WHEN upper(status) = 'SEKOLAH' THEN 'SEKOLAH'
        WHEN upper(status) = 'PULANG' THEN 'PULANG'
        WHEN upper(status) = 'TERLAMBAT' THEN 'TERLAMBAT'
        ELSE upper(status)
      END
    ) ORDER BY tanggal, kegiatan, sesi
  ) INTO v_result
  FROM all_absensi;

  RETURN json_build_object(
    'santri_nis', p_santri_nis,
    'santri_nama', v_santri_nama,
    'periode', to_char(p_start_date, 'DD-MM-YYYY') || ' - ' || to_char(v_end_date, 'DD-MM-YYYY'),
    'data', COALESCE(v_result, '[]'::json)
  );
END;
$function$;

REVOKE ALL ON FUNCTION public.get_ringkasan_absensi_mingguan(text, date) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.get_ringkasan_absensi_mingguan(text, date) FROM anon;
GRANT EXECUTE ON FUNCTION public.get_ringkasan_absensi_mingguan(text, date) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_ringkasan_absensi_mingguan(text, date) TO service_role;
