package com.alhasanah.alhasanahmedia.domain.falak

import com.alhasanah.alhasanahmedia.data.model.falak.FalakEphemerisHarian
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GerhanaBulanEphemerisCalculator(
    private val pembandingMeeusCalculator: JeanMeeusGerhanaBulanCalculator = JeanMeeusGerhanaBulanCalculator(),
) {

    fun hitung(
        konteks: KonteksGerhanaBulan,
        ephemerisHarian: List<FalakEphemerisHarian>,
    ): HasilGerhanaBulanEphemeris {
        val ephemerisPerTanggal = ephemerisHarian.associateBy { LocalDate.parse(it.date) }
        val pembandingMeeus = pembandingMeeusCalculator.prakiraanPurnamaTerdekat(konteks.tanggalKemungkinanGerhanaMasehi)
        val dataIstiqbal = tentukanDataIstiqbal(konteks, ephemerisPerTanggal)
        val sabaq = tentukanSabaq(dataIstiqbal)
        val saatIstiqbal = hitungSaatIstiqbal(konteks, dataIstiqbal, sabaq)
        val statusKemungkinan = tentukanStatusKemungkinan(dataIstiqbal)
        val dataInterpolasi = hitungDataInterpolasi(konteks, ephemerisPerTanggal, saatIstiqbal)
        val bayanganBumi = hitungBayanganBumi(dataInterpolasi)
        val simpul = hitungSimpul(dataInterpolasi, sabaq)
        val klasifikasi = tentukanKlasifikasi(dataInterpolasi, bayanganBumi, statusKemungkinan)
        val jarakKontak = hitungJarakKontak(dataInterpolasi, bayanganBumi, simpul, klasifikasi)
        val koreksiTengah = hitungKoreksiTengah(konteks, dataInterpolasi, simpul)
        val waktuKontak = hitungWaktuKontak(konteks, saatIstiqbal, klasifikasi, jarakKontak, koreksiTengah)
        val magnitude = hitungMagnitude(dataInterpolasi, bayanganBumi, klasifikasi)
        val kesimpulan = susunKesimpulan(klasifikasi, waktuKontak)

        return HasilGerhanaBulanEphemeris(
            konteks = konteks,
            pembandingMeeus = pembandingMeeus,
            dataIstiqbal = dataIstiqbal,
            sabaq = sabaq,
            saatIstiqbal = saatIstiqbal,
            statusKemungkinan = statusKemungkinan,
            dataInterpolasi = dataInterpolasi,
            bayanganBumi = bayanganBumi,
            simpul = simpul,
            klasifikasi = klasifikasi,
            jarakKontak = jarakKontak,
            koreksiTengah = koreksiTengah,
            waktuKontak = waktuKontak,
            magnitude = magnitude,
            kesimpulan = kesimpulan,
            butirPerhitungan = susunButir(
                konteks,
                dataIstiqbal,
                sabaq,
                saatIstiqbal,
                statusKemungkinan,
                dataInterpolasi,
                bayanganBumi,
                simpul,
                klasifikasi,
                jarakKontak,
                koreksiTengah,
                waktuKontak,
                magnitude,
            )
        )
    }

    private fun tentukanDataIstiqbal(
        konteks: KonteksGerhanaBulan,
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
    ): DataIstiqbalGerhanaBulan {
        val kandidatTanggal = if (konteks.modeData == ModeDataGerhanaBulan.InputManual) {
            (-konteks.rentangPencarianManualHari..konteks.rentangPencarianManualHari).map {
                konteks.tanggalKemungkinanGerhanaMasehi.plusDays(it.toLong())
            }
        } else {
            listOf(
                konteks.tanggalKemungkinanGerhanaMasehi.minusDays(1),
                konteks.tanggalKemungkinanGerhanaMasehi,
                konteks.tanggalKemungkinanGerhanaMasehi.plusDays(1),
            )
        }
        val barisFib = kandidatTanggal
            .mapNotNull { tanggal -> ephemerisPerTanggal[tanggal]?.let { tanggal to it } }
            .flatMap { (tanggal, harian) -> harian.hourlyTable.moon.map { Triple(tanggal, it.hourUt(), it) } }
            .filter { (tanggal, jam, _) -> punyaPasanganJamLengkap(ephemerisPerTanggal, tanggal, jam) }
            .maxByOrNull { it.third.doubleAt("fraction_illumination_percent") ?: Double.NEGATIVE_INFINITY }
            ?: error("Data FIB Bulan sekitar ${konteks.tanggalKemungkinanGerhanaMasehi} tidak tersedia lengkap. Periksa data Bulan dan Matahari pada jam FIB serta jam sesudahnya.")
        val tanggalFib = barisFib.first
        val jamFib = barisFib.second
        val matahariFib = barisEphemeris(ephemerisPerTanggal, tanggalFib, jamFib, TabelEphemeris.MATAHARI)
        val matahariSesudah = barisEphemeris(ephemerisPerTanggal, tanggalFib, jamFib + 1, TabelEphemeris.MATAHARI)
        val bulanSesudah = barisEphemeris(ephemerisPerTanggal, tanggalFib, jamFib + 1, TabelEphemeris.BULAN)

        return DataIstiqbalGerhanaBulan(
            fibTerbesarPersen = nilaiAngka(tanggalFib, jamFib, TabelEphemeris.BULAN, "fraction_illumination_percent", barisFib.third),
            jamFibUt = jamFib,
            elmJamFib = nilaiDerajat(tanggalFib, jamFib, TabelEphemeris.MATAHARI, "apparent_ecliptic_longitude", matahariFib.row),
            elmJamSetelahnya = nilaiDerajat(matahariSesudah.tanggal, matahariSesudah.jamUt, TabelEphemeris.MATAHARI, "apparent_ecliptic_longitude", matahariSesudah.row),
            albJamFib = nilaiDerajat(tanggalFib, jamFib, TabelEphemeris.BULAN, "apparent_longitude", barisFib.third),
            albJamSetelahnya = nilaiDerajat(bulanSesudah.tanggal, bulanSesudah.jamUt, TabelEphemeris.BULAN, "apparent_longitude", bulanSesudah.row),
            lintangBulanJamFib = nilaiDerajat(tanggalFib, jamFib, TabelEphemeris.BULAN, "apparent_latitude", barisFib.third),
        )
    }

    private fun punyaPasanganJamLengkap(
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
        tanggal: LocalDate,
        jamUt: Int,
    ): Boolean =
        runCatching {
            barisEphemeris(ephemerisPerTanggal, tanggal, jamUt, TabelEphemeris.MATAHARI)
            barisEphemeris(ephemerisPerTanggal, tanggal, jamUt + 1, TabelEphemeris.MATAHARI)
            barisEphemeris(ephemerisPerTanggal, tanggal, jamUt + 1, TabelEphemeris.BULAN)
        }.isSuccess

    private fun tentukanSabaq(data: DataIstiqbalGerhanaBulan): SabaqGerhanaBulan {
        val sm = deltaMajuDerajat(data.elmJamFib.nilai, data.elmJamSetelahnya.nilai)
        val sb = deltaMajuDerajat(data.albJamFib.nilai, data.albJamSetelahnya.nilai)
        return SabaqGerhanaBulan(
            sabaqMatahariDerajat = sm,
            sabaqBulanDerajat = sb,
            sabaqBulanMatahariDerajat = sb - sm,
        )
    }

    private fun hitungSaatIstiqbal(
        konteks: KonteksGerhanaBulan,
        data: DataIstiqbalGerhanaBulan,
        sabaq: SabaqGerhanaBulan,
    ): SaatIstiqbalGerhanaBulan {
        val mb = normalisasiDerajat(data.elmJamFib.nilai - (data.albJamFib.nilai - 180.0))
        val mbDekat = if (mb > 180.0) mb - 360.0 else mb
        val titikIstiqbal = mbDekat / sabaq.sabaqBulanMatahariDerajat
        val jamUt = data.jamFibUt + titikIstiqbal - konteks.koreksiIstiqbalJam
        val tanggalFib = data.fibTerbesarPersen.sumber.tanggal
        return SaatIstiqbalGerhanaBulan(
            mbDerajat = abs(mbDekat),
            titikIstiqbalJam = titikIstiqbal,
            koreksiIstiqbalJam = konteks.koreksiIstiqbalJam,
            waktuUt = normalisasiWaktu(tanggalFib, jamUt, "GMT/UT"),
            waktuLokal = normalisasiWaktu(tanggalFib, jamUt + konteks.zonaWaktu.offsetJam, konteks.zonaWaktu.nama),
        )
    }

    private fun tentukanStatusKemungkinan(data: DataIstiqbalGerhanaBulan): StatusKemungkinanGerhanaBulan {
        val beta = abs(data.lintangBulanJamFib.nilai)
        val status = when {
            beta > dms(1, 36, 38.0) -> "Tidak terjadi gerhana karena lintang Bulan terlalu jauh dari bidang ekliptika."
            beta > dms(1, 26, 19.0) -> "Kemungkinan gerhana penumbra."
            beta > dms(1, 3, 46.0) -> "Terjadi gerhana penumbra; belum memasuki bayangan inti."
            beta > dms(0, 53, 26.0) -> "Gerhana penumbra terjadi dan gerhana umbra perlu diuji dengan data istiqbal."
            else -> "Gerhana umbra terjadi menurut batas lintang awal."
        }
        return StatusKemungkinanGerhanaBulan(
            nilaiMutlakLintangDerajat = beta,
            status = status,
            memungkinkanGerhana = beta <= dms(1, 36, 38.0),
        )
    }

    private fun hitungDataInterpolasi(
        konteks: KonteksGerhanaBulan,
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
        saatIstiqbal: SaatIstiqbalGerhanaBulan,
    ): DataIstiqbalTerinterpolasiGerhanaBulan =
        DataIstiqbalTerinterpolasiGerhanaBulan(
            semiDiameterBulan = interpolasiIstiqbal(konteks, ephemerisPerTanggal, saatIstiqbal, TabelEphemeris.BULAN, "semi_diameter"),
            horizontalParallaxBulan = interpolasiIstiqbal(konteks, ephemerisPerTanggal, saatIstiqbal, TabelEphemeris.BULAN, "horizontal_parallax"),
            lintangBulan = interpolasiIstiqbal(konteks, ephemerisPerTanggal, saatIstiqbal, TabelEphemeris.BULAN, "apparent_latitude"),
            semiDiameterMatahari = interpolasiIstiqbal(konteks, ephemerisPerTanggal, saatIstiqbal, TabelEphemeris.MATAHARI, "semi_diameter"),
            jarakBumiMatahari = interpolasiAngkaIstiqbal(
                konteks,
                ephemerisPerTanggal,
                saatIstiqbal,
                TabelEphemeris.MATAHARI,
                "true_geocentric_distance_au",
                "AU"
            ),
        )

    private fun hitungBayanganBumi(data: DataIstiqbalTerinterpolasiGerhanaBulan): BayanganBumiGerhanaBulan {
        val hpMatahari = asinDeg(sinDeg(8.794 / 3600.0) / data.jarakBumiMatahari.hasil)
        val parallaxBulan = data.horizontalParallaxBulan.hasilDerajat
        val d = (parallaxBulan + hpMatahari - data.semiDiameterMatahari.hasilDerajat) * 1.02
        return BayanganBumiGerhanaBulan(
            parallaxMatahariDerajat = hpMatahari,
            parallaxBulanDerajat = parallaxBulan,
            semiDiameterBayanganIntiDerajat = d,
            xDerajat = d + data.semiDiameterBulan.hasilDerajat,
            yDerajat = d - data.semiDiameterBulan.hasilDerajat,
        )
    }

    private fun hitungSimpul(
        data: DataIstiqbalTerinterpolasiGerhanaBulan,
        sabaq: SabaqGerhanaBulan,
    ): SimpulGerhanaBulan {
        val lintang = data.lintangBulan.hasilDerajat
        val h = asinDeg(sinDeg(lintang) / sinDeg(5.0))
        val u = abs(atanDeg(tanDeg(lintang) / sinDeg(h)))
        val z = abs(asinDeg(sinDeg(u) * sinDeg(h)))
        return SimpulGerhanaBulan(
            hDerajat = h,
            uDerajat = u,
            zDerajat = z,
            kDerajatPerJam = cosDeg(z) * sabaq.sabaqBulanMatahariDerajat / cosDeg(u),
        )
    }

    private fun tentukanKlasifikasi(
        data: DataIstiqbalTerinterpolasiGerhanaBulan,
        bayangan: BayanganBumiGerhanaBulan,
        statusKemungkinan: StatusKemungkinanGerhanaBulan,
    ): KlasifikasiGerhanaBulan {
        val beta = abs(data.lintangBulan.hasilDerajat)
        val sd = data.semiDiameterBulan.hasilDerajat
        val f1 = 1.02 * (bayangan.parallaxBulanDerajat + data.semiDiameterMatahari.hasilDerajat + bayangan.parallaxMatahariDerajat)
        val f2 = bayangan.semiDiameterBayanganIntiDerajat
        val jenis = when {
            !statusKemungkinan.memungkinkanGerhana -> JenisGerhanaBulan.TidakTerjadi
            f2 > beta + sd -> JenisGerhanaBulan.Total
            beta < f2 + sd && f2 < beta + sd -> JenisGerhanaBulan.Sebagian
            beta < f1 + sd && f1 < beta + sd -> JenisGerhanaBulan.PenumbraSebagian
            f1 > beta + sd && beta > f2 + sd -> JenisGerhanaBulan.PenumbraTotal
            else -> JenisGerhanaBulan.TidakTerjadi
        }
        return KlasifikasiGerhanaBulan(
            jenis = jenis,
            keterangan = when (jenis) {
                JenisGerhanaBulan.Total -> "Gerhana Bulan Total."
                JenisGerhanaBulan.Sebagian -> "Gerhana Bulan Sebagian."
                JenisGerhanaBulan.PenumbraSebagian -> "Gerhana Bulan Penumbra Sebagian."
                JenisGerhanaBulan.PenumbraTotal -> "Gerhana Bulan Penumbra Total."
                JenisGerhanaBulan.TidakTerjadi -> "Tidak terjadi gerhana berdasarkan batas bayangan."
            },
            memenuhiKontakUmbra = jenis == JenisGerhanaBulan.Sebagian || jenis == JenisGerhanaBulan.Total,
            memenuhiKontakTotal = jenis == JenisGerhanaBulan.Total,
        )
    }

    private fun hitungJarakKontak(
        data: DataIstiqbalTerinterpolasiGerhanaBulan,
        bayangan: BayanganBumiGerhanaBulan,
        simpul: SimpulGerhanaBulan,
        klasifikasi: KlasifikasiGerhanaBulan,
    ): JarakKontakGerhanaBulan {
        val c = acosDeg(clamp(cosDeg(bayangan.xDerajat) / cosDeg(simpul.zDerajat)))
        val e = if (klasifikasi.memenuhiKontakTotal && bayangan.yDerajat > simpul.zDerajat) {
            acosDeg(clamp(cosDeg(bayangan.yDerajat) / cosDeg(simpul.zDerajat)))
        } else {
            null
        }
        return JarakKontakGerhanaBulan(
            cDerajat = c,
            eDerajat = e,
            t1Jam = c / simpul.kDerajatPerJam,
            t2Jam = e?.let { it / simpul.kDerajatPerJam },
        )
    }

    private fun hitungKoreksiTengah(
        konteks: KonteksGerhanaBulan,
        data: DataIstiqbalTerinterpolasiGerhanaBulan,
        simpul: SimpulGerhanaBulan,
    ): KoreksiTengahGerhanaBulan {
        val ta = cosDeg(simpul.hDerajat) / sinDeg(simpul.kDerajatPerJam)
        val tb = sinDeg(data.lintangBulan.hasilDerajat) / sinDeg(simpul.kDerajatPerJam)
        val t0 = abs(sinDeg(0.05) * ta * tb) / 10.0
        val t = (konteks.tanggalKemungkinanGerhanaMasehi.year - 2000.0) / 100.0
        val deltaT = (102.3 + 123.5 * t + 32.5 * t * t) / 3600.0
        return KoreksiTengahGerhanaBulan(ta = ta, tb = tb, t0Jam = t0, deltaTJam = deltaT)
    }

    private fun hitungWaktuKontak(
        konteks: KonteksGerhanaBulan,
        saatIstiqbal: SaatIstiqbalGerhanaBulan,
        klasifikasi: KlasifikasiGerhanaBulan,
        jarakKontak: JarakKontakGerhanaBulan,
        koreksiTengah: KoreksiTengahGerhanaBulan,
    ): WaktuKontakGerhanaBulan {
        val tengahUtJam = saatIstiqbal.waktuUt.jamDesimal - koreksiTengah.t0Jam - koreksiTengah.deltaTJam
        val tengahUt = normalisasiWaktu(saatIstiqbal.waktuUt.tanggal, tengahUtJam, "GMT/UT")
        val tengahLokal = normalisasiWaktu(tengahUt.tanggal, tengahUt.jamDesimal + konteks.zonaWaktu.offsetJam, konteks.zonaWaktu.nama)
        fun lokal(offsetJam: Double): WaktuFalak =
            normalisasiWaktu(tengahUt.tanggal, tengahUt.jamDesimal + konteks.zonaWaktu.offsetJam + offsetJam, konteks.zonaWaktu.nama)
        val adaGerhana = klasifikasi.jenis != JenisGerhanaBulan.TidakTerjadi
        return WaktuKontakGerhanaBulan(
            tengahGerhanaUt = tengahUt,
            tengahGerhanaLokal = tengahLokal,
            mulaiGerhanaLokal = if (adaGerhana) lokal(-jarakKontak.t1Jam) else null,
            mulaiTotalLokal = if (klasifikasi.memenuhiKontakTotal) jarakKontak.t2Jam?.let { lokal(-it) } else null,
            selesaiTotalLokal = if (klasifikasi.memenuhiKontakTotal) jarakKontak.t2Jam?.let { lokal(it) } else null,
            selesaiGerhanaLokal = if (adaGerhana) lokal(jarakKontak.t1Jam) else null,
        )
    }

    private fun hitungMagnitude(
        data: DataIstiqbalTerinterpolasiGerhanaBulan,
        bayangan: BayanganBumiGerhanaBulan,
        klasifikasi: KlasifikasiGerhanaBulan,
    ): MagnitudeGerhanaBulan {
        if (!klasifikasi.memenuhiKontakUmbra) return MagnitudeGerhanaBulan(null)
        val beta = abs(data.lintangBulan.hasilDerajat)
        val sd = data.semiDiameterBulan.hasilDerajat
        return MagnitudeGerhanaBulan(
            magnitudeUmbra = ((bayangan.semiDiameterBayanganIntiDerajat + sd) - beta) / (2.0 * sd)
        )
    }

    private fun susunKesimpulan(
        klasifikasi: KlasifikasiGerhanaBulan,
        waktu: WaktuKontakGerhanaBulan,
    ): KesimpulanGerhanaBulan =
        KesimpulanGerhanaBulan(
            jenis = klasifikasi.jenis,
            status = if (klasifikasi.jenis == JenisGerhanaBulan.TidakTerjadi) {
                klasifikasi.keterangan
            } else {
                "${klasifikasi.keterangan} Tengah gerhana ${formatWaktu(waktu.tengahGerhanaLokal)}."
            },
            tengahGerhanaLokal = if (klasifikasi.jenis == JenisGerhanaBulan.TidakTerjadi) null else waktu.tengahGerhanaLokal,
            mulaiGerhanaLokal = waktu.mulaiGerhanaLokal,
            mulaiTotalLokal = waktu.mulaiTotalLokal,
            selesaiTotalLokal = waktu.selesaiTotalLokal,
            selesaiGerhanaLokal = waktu.selesaiGerhanaLokal,
        )

    private fun susunButir(
        konteks: KonteksGerhanaBulan,
        data: DataIstiqbalGerhanaBulan,
        sabaq: SabaqGerhanaBulan,
        saat: SaatIstiqbalGerhanaBulan,
        status: StatusKemungkinanGerhanaBulan,
        interpolasi: DataIstiqbalTerinterpolasiGerhanaBulan,
        bayangan: BayanganBumiGerhanaBulan,
        simpul: SimpulGerhanaBulan,
        klasifikasi: KlasifikasiGerhanaBulan,
        kontak: JarakKontakGerhanaBulan,
        koreksi: KoreksiTengahGerhanaBulan,
        waktu: WaktuKontakGerhanaBulan,
        magnitude: MagnitudeGerhanaBulan,
    ): List<ButirPerhitunganFalak> = listOf(
        butir(1, "Tanggal dan zona waktu", "Input tanggal kemungkinan gerhana dan zona waktu.", "${formatTanggal(konteks.tanggalKemungkinanGerhanaMasehi)}; ${konteks.zonaWaktu.nama}", konteks.bulanHijriah),
        butir(2, "FIB terbesar dan data ephemeris", "Ambil FIB terbesar, ALB, ELM, dan lintang Bulan pada jam FIB.", "FIB=${formatAngka(data.fibTerbesarPersen.nilai)}%; jam ${data.jamFibUt} GMT/UT", "Lintang Bulan=${formatDerajat(data.lintangBulanJamFib.nilai)}", sumber = listOf(data.fibTerbesarPersen.sumber, data.albJamFib.sumber, data.elmJamFib.sumber, data.lintangBulanJamFib.sumber)),
        butir(3, "Sabaq Matahari dan Sabaq Bulan", "SM=ELM berikutnya-ELM FIB; SB=ALB berikutnya-ALB FIB.", "SM=${formatDerajat(sabaq.sabaqMatahariDerajat)}; SB=${formatDerajat(sabaq.sabaqBulanDerajat)}", "SB-SM=${formatDerajat(sabaq.sabaqBulanMatahariDerajat)}"),
        butir(4, "Saat istiqbal", "Istiqbal=jam FIB + MB/(SB-SM) - koreksi istiqbal.", "MB=${formatDerajat(saat.mbDerajat)}; titik=${formatDurasi(saat.titikIstiqbalJam)}; koreksi=${formatDurasi(saat.koreksiIstiqbalJam)}", "${formatWaktu(saat.waktuUt)} / ${formatWaktu(saat.waktuLokal)}"),
        butir(5, "Uji lintang awal", "|Lintang Bulan jam FIB| dibandingkan batas kemungkinan gerhana.", "|L|=${formatDerajat(status.nilaiMutlakLintangDerajat)}", status.status),
        butirInterpolasi(6, "Semi Diameter Bulan saat istiqbal", interpolasi.semiDiameterBulan),
        butirInterpolasi(7, "Horizontal Parallax Bulan saat istiqbal", interpolasi.horizontalParallaxBulan),
        butirInterpolasi(8, "Apparent Latitude Bulan saat istiqbal", interpolasi.lintangBulan),
        butirInterpolasi(9, "Semi Diameter Matahari saat istiqbal", interpolasi.semiDiameterMatahari),
        butir(10, "Jarak Bumi-Matahari saat istiqbal", "Interpolasi True Geocentric Distance Matahari.", substitusiInterpolasiAngka(interpolasi.jarakBumiMatahari), formatAngka(interpolasi.jarakBumiMatahari.hasil), sumber = listOf(interpolasi.jarakBumiMatahari.nilaiAtas.sumber, interpolasi.jarakBumiMatahari.nilaiBawah.sumber)),
        butir(11, "Horizontal Parallax Matahari", "sin HP Matahari = sin 8.794\" / jarak Bumi-Matahari.", "sin 8.794\" / ${formatAngka(interpolasi.jarakBumiMatahari.hasil)}", formatDerajat(bayangan.parallaxMatahariDerajat)),
        butir(12, "Jarak Bulan dari titik simpul (H)", "sin H = sin L Bulan / sin 5°.", "sin ${formatDerajat(interpolasi.lintangBulan.hasilDerajat)} / sin 5°", formatDerajat(simpul.hDerajat)),
        butir(13, "Lintang Bulan maksimum terkoreksi (U)", "tan U = tan L Bulan / sin H.", "tan ${formatDerajat(interpolasi.lintangBulan.hasilDerajat)} / sin ${formatDerajat(simpul.hDerajat)}", formatDerajat(simpul.uDerajat)),
        butir(14, "Lintang Bulan minimum terkoreksi (Z)", "sin Z = sin U x sin H.", "sin ${formatDerajat(simpul.uDerajat)} x sin ${formatDerajat(simpul.hDerajat)}", formatDerajat(simpul.zDerajat)),
        butir(15, "Koreksi kecepatan Bulan relatif Matahari (K)", "K = cos Z x (SB-SM) / cos U.", "cos ${formatDerajat(simpul.zDerajat)} x ${formatDerajat(sabaq.sabaqBulanMatahariDerajat)} / cos ${formatDerajat(simpul.uDerajat)}", formatDerajat(simpul.kDerajatPerJam)),
        butir(16, "Semi Diameter bayangan inti Bumi (D)", "D=(HP Bulan+HP Matahari-SD Matahari) x 1.02.", "(${formatDerajat(bayangan.parallaxBulanDerajat)} + ${formatDerajat(bayangan.parallaxMatahariDerajat)} - ${formatDerajat(interpolasi.semiDiameterMatahari.hasilDerajat)}) x 1.02", formatDerajat(bayangan.semiDiameterBayanganIntiDerajat)),
        butir(17, "Jarak kontak luar umbra (X)", "X=D+SD Bulan.", "${formatDerajat(bayangan.semiDiameterBayanganIntiDerajat)} + ${formatDerajat(interpolasi.semiDiameterBulan.hasilDerajat)}", formatDerajat(bayangan.xDerajat)),
        butir(18, "Jarak kontak total (Y)", "Y=D-SD Bulan.", "${formatDerajat(bayangan.semiDiameterBayanganIntiDerajat)} - ${formatDerajat(interpolasi.semiDiameterBulan.hasilDerajat)}", formatDerajat(bayangan.yDerajat)),
        butir(19, "Jenis gerhana", "Bandingkan beta, SD Bulan, dan batas bayangan.", "beta=${formatDerajat(abs(interpolasi.lintangBulan.hasilDerajat))}; Z=${formatDerajat(simpul.zDerajat)}; Y=${formatDerajat(bayangan.yDerajat)}", klasifikasi.keterangan),
        butir(20, "Jarak C", "cos C = cos X / cos Z.", "cos ${formatDerajat(bayangan.xDerajat)} / cos ${formatDerajat(simpul.zDerajat)}", formatDerajat(kontak.cDerajat)),
        butir(21, "T1", "T1=C/K.", "${formatDerajat(kontak.cDerajat)} / ${formatDerajat(simpul.kDerajatPerJam)}", formatDurasi(kontak.t1Jam)),
        butir(22, "Jarak E", "cos E = cos Y / cos Z.", if (kontak.eDerajat != null) "cos ${formatDerajat(bayangan.yDerajat)} / cos ${formatDerajat(simpul.zDerajat)}" else "Tidak dipakai karena bukan gerhana total.", kontak.eDerajat?.let(::formatDerajat) ?: "-"),
        butir(23, "T2", "T2=E/K.", kontak.eDerajat?.let { "${formatDerajat(it)} / ${formatDerajat(simpul.kDerajatPerJam)}" } ?: "Tidak dipakai.", kontak.t2Jam?.let(::formatDurasi) ?: "-"),
        butir(24, "Ta", "Ta=cos H / sin K.", "cos ${formatDerajat(simpul.hDerajat)} / sin ${formatDerajat(simpul.kDerajatPerJam)}", formatAngka(koreksi.ta)),
        butir(25, "Tb", "Tb=sin L Bulan / sin K.", "sin ${formatDerajat(interpolasi.lintangBulan.hasilDerajat)} / sin ${formatDerajat(simpul.kDerajatPerJam)}", formatAngka(koreksi.tb)),
        butir(
            26,
            "T0",
            "T0=|sin 0.05° x Ta x Tb| dengan skala contoh ephemeris.",
            "sin 0.05° x ${formatAngka(koreksi.ta)} x ${formatAngka(koreksi.tb)} / 10",
            formatDurasi(koreksi.t0Jam)
        ),
        butir(27, "Delta T", "Delta T=(102.3 + 123.5t + 32.5t^2) / 3600; t=(tahun-2000)/100.", "tahun=${konteks.tanggalKemungkinanGerhanaMasehi.year}", formatDurasi(koreksi.deltaTJam)),
        butir(28, "Tengah gerhana", "Tgh=Istiqbal - T0 - Delta T.", "${formatWaktu(saat.waktuUt)} - ${formatDurasi(koreksi.t0Jam)} - ${formatDurasi(koreksi.deltaTJam)}", "${formatWaktu(waktu.tengahGerhanaUt)} / ${formatWaktu(waktu.tengahGerhanaLokal)}"),
        butir(29, "Mulai gerhana", "Mulai Gerhana=Tgh-T1.", "Tgh - ${formatDurasi(kontak.t1Jam)}", waktu.mulaiGerhanaLokal?.let(::formatWaktu) ?: "-"),
        butir(30, "Mulai total", "Mulai Total=Tgh-T2.", kontak.t2Jam?.let { "Tgh - ${formatDurasi(it)}" } ?: "Tidak dipakai.", waktu.mulaiTotalLokal?.let(::formatWaktu) ?: "-"),
        butir(31, "Selesai total", "Selesai Total=Tgh+T2.", kontak.t2Jam?.let { "Tgh + ${formatDurasi(it)}" } ?: "Tidak dipakai.", waktu.selesaiTotalLokal?.let(::formatWaktu) ?: "-"),
        butir(32, "Selesai gerhana", "Selesai Gerhana=Tgh+T1.", "Tgh + ${formatDurasi(kontak.t1Jam)}", waktu.selesaiGerhanaLokal?.let(::formatWaktu) ?: "-"),
        butir(33, "Magnitude umbra", "Magnitude=((D+SD Bulan)-|beta|)/(2 x SD Bulan).", "D=${formatDerajat(bayangan.semiDiameterBayanganIntiDerajat)}; SD=${formatDerajat(interpolasi.semiDiameterBulan.hasilDerajat)}; beta=${formatDerajat(abs(interpolasi.lintangBulan.hasilDerajat))}", magnitude.magnitudeUmbra?.let(::formatAngka) ?: "-"),
    )

    private fun butir(
        nomor: Int,
        judul: String,
        rumus: String,
        substitusi: String,
        hasil: String,
        catatan: String? = null,
        sumber: List<SumberEphemerisFalak> = emptyList(),
    ) = ButirPerhitunganFalak(nomor, judul, rumus, substitusi, hasil, catatan, sumber)

    private fun butirInterpolasi(nomor: Int, judul: String, interpolasi: InterpolasiEphemerisFalak): ButirPerhitunganFalak =
        butir(
            nomor = nomor,
            judul = judul,
            rumus = "Nilai atas - (nilai atas - nilai bawah) x pecahan jam.",
            substitusi = substitusiInterpolasi(interpolasi),
            hasil = formatDerajat(interpolasi.hasilDerajat),
            catatan = "Pecahan jam dihitung dari saat istiqbal GMT/UT.",
            sumber = listOf(interpolasi.nilaiAtas.sumber, interpolasi.nilaiBawah.sumber)
        )

    private fun interpolasiIstiqbal(
        konteks: KonteksGerhanaBulan,
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
        saatIstiqbal: SaatIstiqbalGerhanaBulan,
        tabel: TabelEphemeris,
        kolom: String,
    ): InterpolasiEphemerisFalak {
        val jamUt = saatIstiqbal.waktuLokal.jamDesimal - konteks.zonaWaktu.offsetJam
        val waktuUt = normalisasiWaktu(saatIstiqbal.waktuLokal.tanggal, jamUt, "GMT/UT")
        val jamAtas = floor(waktuUt.jamDesimal).toInt()
        val jamBawah = jamAtas + 1
        val nc = waktuUt.jamDesimal - jamAtas
        val atas = barisEphemeris(ephemerisPerTanggal, waktuUt.tanggal, jamAtas, tabel)
        val bawah = barisEphemeris(ephemerisPerTanggal, waktuUt.tanggal, jamBawah, tabel)
        val nilaiAtas = nilaiDerajat(atas.tanggal, atas.jamUt, tabel, kolom, atas.row)
        val nilaiBawah = nilaiDerajat(bawah.tanggal, bawah.jamUt, tabel, kolom, bawah.row)
        return InterpolasiEphemerisFalak(
            jamAtasUt = atas.jamUt,
            jamBawahUt = bawah.jamUt,
            nc = nc,
            nilaiAtas = nilaiAtas,
            nilaiBawah = nilaiBawah,
            hasilDerajat = nilaiAtas.nilai - (nilaiAtas.nilai - nilaiBawah.nilai) * nc,
        )
    }

    private fun interpolasiAngkaIstiqbal(
        konteks: KonteksGerhanaBulan,
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
        saatIstiqbal: SaatIstiqbalGerhanaBulan,
        tabel: TabelEphemeris,
        kolom: String,
        satuan: String,
    ): InterpolasiAngkaEphemerisFalak {
        val jamUt = saatIstiqbal.waktuLokal.jamDesimal - konteks.zonaWaktu.offsetJam
        val waktuUt = normalisasiWaktu(saatIstiqbal.waktuLokal.tanggal, jamUt, "GMT/UT")
        val jamAtas = floor(waktuUt.jamDesimal).toInt()
        val jamBawah = jamAtas + 1
        val nc = waktuUt.jamDesimal - jamAtas
        val atas = barisEphemeris(ephemerisPerTanggal, waktuUt.tanggal, jamAtas, tabel)
        val bawah = barisEphemeris(ephemerisPerTanggal, waktuUt.tanggal, jamBawah, tabel)
        val nilaiAtas = nilaiAngka(atas.tanggal, atas.jamUt, tabel, kolom, atas.row)
        val nilaiBawah = nilaiAngka(bawah.tanggal, bawah.jamUt, tabel, kolom, bawah.row)
        return InterpolasiAngkaEphemerisFalak(
            jamAtasUt = atas.jamUt,
            jamBawahUt = bawah.jamUt,
            nc = nc,
            nilaiAtas = nilaiAtas,
            nilaiBawah = nilaiBawah,
            hasil = nilaiAtas.nilai - (nilaiAtas.nilai - nilaiBawah.nilai) * nc,
            satuan = satuan,
        )
    }

    private fun barisEphemeris(
        ephemerisPerTanggal: Map<LocalDate, FalakEphemerisHarian>,
        tanggalAwal: LocalDate,
        jamUt: Int,
        tabel: TabelEphemeris,
    ): BarisEphemeris {
        if (jamUt == 24) {
            val rowsTanggalAwal = when (tabel) {
                TabelEphemeris.MATAHARI -> ephemerisPerTanggal[tanggalAwal]?.hourlyTable?.sun
                TabelEphemeris.BULAN -> ephemerisPerTanggal[tanggalAwal]?.hourlyTable?.moon
            }
            val row24 = rowsTanggalAwal?.firstOrNull { it.hourUt() == 24 }
            if (row24 != null) return BarisEphemeris(tanggalAwal, 24, row24)
        }
        val tanggal = tanggalAwal.plusDays(Math.floorDiv(jamUt, 24).toLong())
        val jam = Math.floorMod(jamUt, 24)
        val dataTanggal = ephemerisPerTanggal[tanggal] ?: error("Data ephemeris tanggal $tanggal tidak tersedia.")
        val rows = if (tabel == TabelEphemeris.MATAHARI) dataTanggal.hourlyTable.sun else dataTanggal.hourlyTable.moon
        val row = rows.firstOrNull { it.hourUt() == jam }
            ?: rows.firstOrNull { it.hourUt() == jamUt }
            ?: error("Data ${tabel.label} jam $jam GMT/UT tanggal $tanggal tidak tersedia.")
        return BarisEphemeris(tanggal, row.hourUt(), row)
    }

    private fun nilaiDerajat(tanggal: LocalDate, jamUt: Int, tabel: TabelEphemeris, kolom: String, row: JsonObject): NilaiEphemerisFalak {
        val obj = row[kolom]?.jsonObjectOrNull() ?: error("Kolom $kolom tidak tersedia pada ${tabel.label} jam $jamUt GMT/UT.")
        val value = obj.doubleAt("decimal_degree") ?: error("Nilai decimal_degree kolom $kolom tidak tersedia.")
        return NilaiEphemerisFalak(value, obj.textAt("raw"), SumberEphemerisFalak(tanggal, jamUt, tabel.label, kolom, obj.textAt("raw")))
    }

    private fun nilaiAngka(tanggal: LocalDate, jamUt: Int, tabel: TabelEphemeris, kolom: String, row: JsonObject): NilaiEphemerisFalak {
        val kolomAktual = if (row.doubleAt(kolom) != null) {
            kolom
        } else if (kolom == "true_geocentric_distance_au" && row.doubleAt("true_geocentric_distance") != null) {
            "true_geocentric_distance"
        } else {
            kolom
        }
        val value = row.doubleAt(kolomAktual) ?: error("Nilai $kolom tidak tersedia pada ${tabel.label} jam $jamUt GMT/UT.")
        return NilaiEphemerisFalak(value, row.textAt(kolomAktual), SumberEphemerisFalak(tanggal, jamUt, tabel.label, kolomAktual, row.textAt(kolomAktual)))
    }

    private fun normalisasiWaktu(tanggalAwal: LocalDate, jam: Double, zona: String): WaktuFalak {
        val days = floor(jam / 24.0).toLong()
        var normalizedHour = jam - (days * 24.0)
        var date = tanggalAwal.plusDays(days)
        if (normalizedHour < 0.0) {
            normalizedHour += 24.0
            date = date.minusDays(1)
        }
        return WaktuFalak(date, normalizedHour, zona)
    }

    private fun JsonObject.hourUt(): Int =
        this["hour_ut"]?.jsonPrimitive?.intOrNull ?: error("hour_ut tidak tersedia.")

    private fun JsonObject.doubleAt(name: String): Double? =
        this[name]?.doubleContentOrNull()

    private fun JsonObject.textAt(name: String): String? =
        this[name]?.jsonPrimitiveOrNull()?.contentOrNull

    private fun JsonElement.doubleContentOrNull(): Double? =
        runCatching { jsonPrimitive.doubleOrNull ?: jsonPrimitive.content.replace(",", ".").toDoubleOrNull() }.getOrNull()

    private fun JsonElement.jsonObjectOrNull(): JsonObject? =
        runCatching { jsonObject }.getOrNull()

    private fun JsonElement.jsonPrimitiveOrNull() =
        runCatching { jsonPrimitive }.getOrNull()

    private fun deltaMajuDerajat(awal: Double, setelah: Double): Double {
        var delta = setelah - awal
        while (delta < 0.0) delta += 360.0
        return delta
    }

    private fun normalisasiDerajat(value: Double): Double {
        var normalized = value % 360.0
        if (normalized < 0.0) normalized += 360.0
        return normalized
    }

    private fun clamp(value: Double): Double = value.coerceIn(-1.0, 1.0)

    private fun dms(degree: Int, minute: Int, second: Double): Double =
        degree + minute / 60.0 + second / 3600.0

    private fun sinDeg(value: Double): Double = sin(value * PI / 180.0)

    private fun cosDeg(value: Double): Double = cos(value * PI / 180.0)

    private fun tanDeg(value: Double): Double = tan(value * PI / 180.0)

    private fun asinDeg(value: Double): Double = asin(clamp(value)) * 180.0 / PI

    private fun acosDeg(value: Double): Double = acos(clamp(value)) * 180.0 / PI

    private fun atanDeg(value: Double): Double = kotlin.math.atan(value) * 180.0 / PI

    private fun formatDerajat(value: Double): String {
        val sign = if (value < 0.0) "-" else ""
        val totalCentiseconds = (abs(value) * 3600.0 * 100.0).roundToLong()
        val degree = totalCentiseconds / 360000
        val minute = (totalCentiseconds % 360000) / 6000
        val second = (totalCentiseconds % 6000) / 100.0
        return "$sign${degree}° %02d' %05.2f\"".format(Locale.US, minute, second)
    }

    private fun formatDurasi(value: Double): String {
        val sign = if (value < 0.0) "-" else ""
        val totalCentiseconds = (abs(value) * 3600.0 * 100.0).roundToLong()
        val hour = totalCentiseconds / 360000
        val minute = (totalCentiseconds % 360000) / 6000
        val second = (totalCentiseconds % 6000) / 100.0
        return "$sign%02dh %02dm %05.2fs".format(Locale.US, hour, minute, second)
    }

    private fun formatWaktu(value: WaktuFalak): String =
        "${formatTanggal(value.tanggal)} ${formatJam(value.jamDesimal)} ${value.zona}"

    private fun formatJam(value: Double): String {
        val totalSeconds = (value * 3600.0).roundToLong()
        val hour = totalSeconds / 3600
        val minute = (totalSeconds % 3600) / 60
        val second = totalSeconds % 60
        return "%02d:%02d:%02d".format(Locale.US, hour, minute, second)
    }

    private fun formatTanggal(tanggal: LocalDate): String =
        tanggal.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID")))

    private fun substitusiInterpolasi(interpolasi: InterpolasiEphemerisFalak): String =
        "${formatDerajat(interpolasi.nilaiAtas.nilai)} - (${formatDerajat(interpolasi.nilaiAtas.nilai)} - ${formatDerajat(interpolasi.nilaiBawah.nilai)}) x ${formatAngka(interpolasi.nc)}"

    private fun substitusiInterpolasiAngka(interpolasi: InterpolasiAngkaEphemerisFalak): String =
        "${formatAngka(interpolasi.nilaiAtas.nilai)} - (${formatAngka(interpolasi.nilaiAtas.nilai)} - ${formatAngka(interpolasi.nilaiBawah.nilai)}) x ${formatAngka(interpolasi.nc)}"

    private fun formatAngka(value: Double): String =
        "%.8f".format(Locale.US, value).trimEnd('0').trimEnd('.')

    private data class BarisEphemeris(
        val tanggal: LocalDate,
        val jamUt: Int,
        val row: JsonObject,
    )

    private enum class TabelEphemeris(val label: String) {
        MATAHARI("Data Matahari"),
        BULAN("Data Bulan"),
    }
}
