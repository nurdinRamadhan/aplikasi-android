package com.alhasanah.alhasanahmedia.util

import com.alhasanah.alhasanahmedia.data.model.weather.BmkgRegion

object BmkgAdm4Catalog {
    val regions: List<BmkgRegion> = listOf(
        BmkgRegion("32.78.04.1003", "Jawa Barat", "Kota Tasikmalaya", "Indihiang", "Indihiang"),
        BmkgRegion("32.78.04.1006", "Jawa Barat", "Kota Tasikmalaya", "Indihiang", "Panyingkiran"),
        BmkgRegion("32.78.01.1001", "Jawa Barat", "Kota Tasikmalaya", "Cihideung", "Argasari"),
        BmkgRegion("32.78.01.1002", "Jawa Barat", "Kota Tasikmalaya", "Cihideung", "Cilembang"),
        BmkgRegion("32.78.02.1001", "Jawa Barat", "Kota Tasikmalaya", "Tawang", "Tawangsari"),
        BmkgRegion("32.78.02.1005", "Jawa Barat", "Kota Tasikmalaya", "Tawang", "Kahuripan"),
        BmkgRegion("32.78.03.1001", "Jawa Barat", "Kota Tasikmalaya", "Cipedes", "Cipedes"),
        BmkgRegion("32.78.03.1004", "Jawa Barat", "Kota Tasikmalaya", "Cipedes", "Nagarasari"),
        BmkgRegion("32.78.05.1001", "Jawa Barat", "Kota Tasikmalaya", "Kawalu", "Kawalu"),
        BmkgRegion("32.78.06.1001", "Jawa Barat", "Kota Tasikmalaya", "Cibeureum", "Setiajaya"),
        BmkgRegion("32.78.07.1001", "Jawa Barat", "Kota Tasikmalaya", "Tamansari", "Tamansari"),
        BmkgRegion("32.78.08.1001", "Jawa Barat", "Kota Tasikmalaya", "Mangkubumi", "Mangkubumi"),
        BmkgRegion("32.78.09.1001", "Jawa Barat", "Kota Tasikmalaya", "Bungursari", "Bungursari"),
        BmkgRegion("32.78.10.1001", "Jawa Barat", "Kota Tasikmalaya", "Purbaratu", "Purbaratu"),
        BmkgRegion("31.71.03.1001", "DKI Jakarta", "Kota Adm. Jakarta Pusat", "Kemayoran", "Kemayoran"),
        BmkgRegion("31.71.01.1001", "DKI Jakarta", "Kota Adm. Jakarta Pusat", "Gambir", "Gambir"),
        BmkgRegion("31.74.06.1001", "DKI Jakarta", "Kota Adm. Jakarta Selatan", "Setiabudi", "Setiabudi"),
        BmkgRegion("31.73.06.1001", "DKI Jakarta", "Kota Adm. Jakarta Barat", "Kebon Jeruk", "Kebon Jeruk"),
        BmkgRegion("31.72.04.1001", "DKI Jakarta", "Kota Adm. Jakarta Utara", "Tanjung Priok", "Tanjung Priok"),
        BmkgRegion("31.75.01.1001", "DKI Jakarta", "Kota Adm. Jakarta Timur", "Matraman", "Pisangan Baru"),
        BmkgRegion("32.73.26.1001", "Jawa Barat", "Kota Bandung", "Coblong", "Dago"),
        BmkgRegion("32.73.06.1001", "Jawa Barat", "Kota Bandung", "Regol", "Ciseureuh"),
        BmkgRegion("33.74.01.1001", "Jawa Tengah", "Kota Semarang", "Semarang Tengah", "Pekunden"),
        BmkgRegion("34.71.05.1001", "DI Yogyakarta", "Kota Yogyakarta", "Gondokusuman", "Baciro"),
        BmkgRegion("35.78.01.1001", "Jawa Timur", "Kota Surabaya", "Tegalsari", "Kedungdoro"),
        BmkgRegion("51.71.03.1001", "Bali", "Kota Denpasar", "Denpasar Barat", "Pemecutan")
    )

    fun search(query: String, limit: Int = 20): List<BmkgRegion> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return regions.take(8)
        return regions.filter { region ->
            normalized in region.adm4.lowercase() ||
                normalized in region.fullLabel.lowercase()
        }.take(limit)
    }

    fun findByAdm4(adm4: String): BmkgRegion? =
        regions.firstOrNull { it.adm4 == adm4 }
}
