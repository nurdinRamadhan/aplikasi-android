-- ==============================================================================
-- SQL OPTIMASI NOTIFIKASI ALHASANAH MEDIA (PREMIUM VERSION)
-- Deskripsi: Mengubah teks notifikasi menjadi lebih informatif, sopan (Islami), 
--            dan mendukung notifikasi pembayaran sukses (LUNAS).
-- Lokasi: Eksekusi di SQL Editor Supabase.
-- ==============================================================================

-- 1. FUNGSI PEMBANTU: FORMAT RUPIAH
-- Agar nominal tagihan mudah dibaca (Contoh: 500.000)
CREATE OR REPLACE FUNCTION public.format_rupiah(nominal numeric) 
RETURNS text AS $$
BEGIN
    RETURN to_char(nominal, 'FM999G999G999G999');
END;
$$ LANGUAGE plpgsql;

-- 2. OPTIMASI NOTIFIKASI PERIZINAN
-- Mendukung mapping status bahasa Inggris ke Indonesia yang sopan.
CREATE OR REPLACE FUNCTION public.tr_notify_perizinan()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
DECLARE
    v_status_indo TEXT;
BEGIN
    -- Mapping Status
    v_status_indo := CASE 
        WHEN NEW.status = 'approved' OR NEW.status = 'DISETUJUI' THEN 'telah DISETUJUI'
        WHEN NEW.status = 'rejected' OR NEW.status = 'DITOLAK' THEN 'mohon maaf, BELUM DISETUJUI'
        WHEN NEW.status = 'pending'  OR NEW.status = 'DIPROSES' THEN 'sedang DIPROSES'
        ELSE 'berstatus ' || NEW.status
    END;

    IF (TG_OP = 'INSERT') OR (OLD.status IS DISTINCT FROM NEW.status) THEN
        PERFORM public.create_notification_for_wali(
            NEW.santri_nis,
            'Update Perizinan Santri',
            'Assalamu''alaikum, permohonan izin ' || NEW.jenis_izin || ' ananda ' || v_status_indo || '.',
            jsonb_build_object('type', 'perizinan', 'nis', NEW.santri_nis, 'id', NEW.id),
            'perizinan_santri'
        );
    END IF;
    RETURN NEW;
END; $function$;

-- 3. OPTIMASI NOTIFIKASI TAGIHAN (KEUANGAN)
-- Sekarang mendukung notifikasi saat tagihan baru dibuat DAN saat pembayaran LUNAS.
CREATE OR REPLACE FUNCTION public.tr_notify_tagihan()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    -- Kondisi A: Tagihan Baru Dibuat
    IF (TG_OP = 'INSERT') THEN
        PERFORM public.create_notification_for_wali(
            NEW.santri_nis,
            'Tagihan Baru',
            'Terdapat tagihan baru: ' || NEW.deskripsi_tagihan || ' sebesar Rp ' || public.format_rupiah(NEW.nominal_tagihan) || '. Mohon segera melakukan pembayaran.',
            jsonb_build_object('type', 'tagihan', 'nis', NEW.santri_nis, 'id', NEW.id),
            'tagihan_santri'
        );
    
    -- Kondisi B: Status Berubah Jadi LUNAS (Pembayaran Berhasil)
    ELSIF (TG_OP = 'UPDATE') AND (OLD.status IS DISTINCT FROM NEW.status) AND (NEW.status = 'LUNAS') THEN
        PERFORM public.create_notification_for_wali(
            NEW.santri_nis,
            'Pembayaran Berhasil',
            'Alhamdulillah, pembayaran ' || NEW.deskripsi_tagihan || ' sebesar Rp ' || public.format_rupiah(NEW.nominal_tagihan) || ' telah kami terima. Syukran wa jazakumullah khairan.',
            jsonb_build_object('type', 'tagihan', 'nis', NEW.santri_nis, 'id', NEW.id),
            'tagihan_santri'
        );
    END IF;
    
    RETURN NEW;
END; $function$;

-- 4. OPTIMASI NOTIFIKASI PELANGGARAN (KEDISIPLINAN)
CREATE OR REPLACE FUNCTION public.tr_notify_pelanggaran()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    PERFORM public.create_notification_for_wali(
        NEW.santri_nis,
        'Catatan Kedisiplinan',
        'Informasi kedisiplinan: Ananda tercatat melakukan ' || NEW.jenis_pelanggaran || '. Mari bimbing ananda untuk menjadi lebih baik.',
        jsonb_build_object('type', 'pelanggaran', 'nis', NEW.santri_nis, 'id', NEW.id),
        'pelanggaran_santri'
    );
    RETURN NEW;
END; $function$;

-- 5. OPTIMASI NOTIFIKASI KESEHATAN
CREATE OR REPLACE FUNCTION public.tr_notify_kesehatan()
 RETURNS trigger
 LANGUAGE plpgsql
AS $function$
BEGIN
    PERFORM public.create_notification_for_wali(
        NEW.santri_nis,
        'Laporan Kesehatan Santri',
        'Laporan kesehatan ananda hari ini: ' || NEW.keluhan || '. Tindakan yang telah diambil: ' || NEW.tindakan || '. Mohon doa untuk kesembuhan ananda.',
        jsonb_build_object('type', 'kesehatan', 'nis', NEW.santri_nis, 'id', NEW.id),
        'kesehatan_santri'
    );
    RETURN NEW;
END; $function$;

-- Catatan: Pastikan Trigger di tabel masing-masing sudah terhubung ke fungsi-fungsi di atas.
-- Jika belum, jalankan perintah di bawah ini (opsional jika trigger sudah ada):
-- DROP TRIGGER IF EXISTS on_tagihan_change ON public.tagihan_santri;
-- CREATE TRIGGER on_tagihan_change AFTER INSERT OR UPDATE ON public.tagihan_santri FOR EACH ROW EXECUTE FUNCTION tr_notify_tagihan();
