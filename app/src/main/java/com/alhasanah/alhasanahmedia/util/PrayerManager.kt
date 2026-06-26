package com.alhasanah.alhasanahmedia.util

import com.alhasanah.alhasanahmedia.data.model.prayer.PrayerLocation
import com.alhasanah.alhasanahmedia.data.model.prayer.PrayerScheduleData
import com.alhasanah.alhasanahmedia.data.model.prayer.PrayerScheduleEntry
import com.alhasanah.alhasanahmedia.data.repository.PrayerScheduleRepository
import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.collect

data class PrayerTimeInfo(
    val name: String,
    val time: String,
    val countdown: String,
    val locationName: String,
    val isEstimated: Boolean = false
)

class PrayerManager(
    private val repository: PrayerScheduleRepository,
    private val locationManager: PrayerLocationManager
) {
    private val zoneId = ZoneId.of("Asia/Jakarta")
    private val scheduleCache = linkedMapOf<String, PrayerScheduleData>()
    private var selectedLocation: PrayerLocation? = null
    private var currentSnapshot: PrayerCoordinateSnapshot? = null
    private var useEstimatedOfflineLocation = false
    private var lastRefreshAt: Long = 0L
    private var lastLocationCheckAt: Long = 0L

    suspend fun getNextPrayer(): PrayerTimeInfo {
        return try {
            refreshScheduleIfNeeded()
            if (useEstimatedOfflineLocation) {
                computeEstimatedPrayer() ?: computeNextPrayer() ?: fallbackPrayerInfo()
            } else {
                computeNextPrayer() ?: computeEstimatedPrayer() ?: fallbackPrayerInfo()
            }
        } catch (e: Exception) {
            computeNextPrayer() ?: computeEstimatedPrayer() ?: fallbackPrayerInfo()
        }
    }

    private suspend fun refreshScheduleIfNeeded() {
        val nowDate = LocalDate.now(zoneId)
        val currentMonth = YearMonth.from(nowDate).toString()
        val nextMonth = YearMonth.from(nowDate.plusDays(1)).toString()

        val locationChanged = refreshLocationIfNeeded()
        val shouldRefresh = locationChanged ||
            scheduleCache.isEmpty() ||
            !scheduleCache.containsKey(currentMonth) ||
            !scheduleCache.hasDate(nowDate.plusDays(1)) ||
            System.currentTimeMillis() - lastRefreshAt > REFRESH_INTERVAL_MILLIS

        if (!shouldRefresh) return

        val location = selectedLocation ?: return
        fetchMonth(location.id, currentMonth)
        if (nextMonth != currentMonth) {
            fetchMonth(location.id, nextMonth)
        }
        lastRefreshAt = System.currentTimeMillis()
    }

    private suspend fun refreshLocationIfNeeded(): Boolean {
        val now = System.currentTimeMillis()
        if (selectedLocation != null && now - lastLocationCheckAt < LOCATION_CHECK_INTERVAL_MILLIS) {
            return false
        }
        lastLocationCheckAt = now

        val resolved = resolveLocation()
        val newLocation = resolved.location ?: return false
        val changed = selectedLocation?.id != newLocation.id
        if (changed) {
            scheduleCache.clear()
            lastRefreshAt = 0L
        }
        selectedLocation = newLocation
        useEstimatedOfflineLocation = resolved.useEstimatedOfflineLocation
        return changed
    }

    private suspend fun resolveLocation(): LocationResolution {
        val snapshot = locationManager.getCurrentLocationSnapshot()
        currentSnapshot = snapshot ?: currentSnapshot
        val saved = locationManager.getSavedPrayerLocation()

        if (saved != null) {
            val distance = snapshot?.let { saved.distanceTo(it) }
            if (snapshot != null && distance == null) {
                val fresh = snapshot.resolveByKeyword()
                if (fresh != null) {
                    locationManager.saveSelectedLocation(fresh, snapshot.latitude, snapshot.longitude)
                    return LocationResolution(fresh, useEstimatedOfflineLocation = false)
                }
            }

            val movedFar = distance?.let { it > MOVEMENT_THRESHOLD_METERS } == true
            if (!movedFar) {
                return LocationResolution(saved.location, useEstimatedOfflineLocation = false)
            }

            val fresh = snapshot.resolveByKeyword()
            if (fresh != null) {
                locationManager.saveSelectedLocation(fresh, snapshot.latitude, snapshot.longitude)
                return LocationResolution(fresh, useEstimatedOfflineLocation = false)
            }

            return LocationResolution(saved.location, useEstimatedOfflineLocation = true)
        }

        val fresh = snapshot.resolveByKeyword() ?: resolveByKeyword(DEFAULT_LOCATION_KEYWORD)
        fresh?.let { location ->
            if (snapshot != null) {
                locationManager.saveSelectedLocation(location, snapshot.latitude, snapshot.longitude)
            } else {
                locationManager.saveSelectedLocation(location)
            }
        }
        return LocationResolution(fresh, useEstimatedOfflineLocation = false)
    }

    private suspend fun PrayerCoordinateSnapshot?.resolveByKeyword(): PrayerLocation? {
        val keyword = this?.keyword?.takeIf { it.isNotBlank() } ?: return null
        return resolveByKeyword(keyword)
    }

    private suspend fun resolveByKeyword(keyword: String): PrayerLocation? {
        var resolved: PrayerLocation? = null
        repository.searchLocations(keyword).collect { result ->
            result.onSuccess { resource ->
                resolved = resource.data.firstOrNull()
            }
        }
        return resolved
    }

    private suspend fun fetchMonth(locationId: String, yearMonth: String) {
        repository.getMonthlySchedule(locationId, yearMonth).collect { result ->
            result.onSuccess { resource ->
                scheduleCache[yearMonth] = resource.data
            }
        }
    }

    private fun computeNextPrayer(): PrayerTimeInfo? {
        val now = LocalDateTime.now(zoneId)
        val today = now.toLocalDate()
        val candidates = buildList {
            addAll(kemenagCandidatesForDate(today))
            addAll(kemenagCandidatesForDate(today.plusDays(1)))
        }
            .filter { it.dateTime.isAfter(now) }
            .sortedBy { it.dateTime }

        val next = candidates.firstOrNull() ?: return null
        val seconds = Duration.between(now, next.dateTime).seconds.coerceAtLeast(0L)
        val location = scheduleCache.values.firstOrNull()?.let { data ->
            listOf(data.kabko, data.prov)
                .filter { it.isNotBlank() }
                .joinToString(", ")
        }.orEmpty().ifBlank {
            selectedLocation?.lokasi ?: DEFAULT_LOCATION_LABEL
        }

        return PrayerTimeInfo(
            name = next.name,
            time = next.time,
            countdown = formatCountdown(seconds),
            locationName = location
        )
    }

    private fun computeEstimatedPrayer(): PrayerTimeInfo? {
        val snapshot = currentSnapshot ?: return null
        val now = LocalDateTime.now(zoneId)
        val today = now.toLocalDate()
        val offsets = calibrationOffsets(snapshot)
        val candidates = buildList {
            addAll(adhanCandidatesForDate(today, snapshot, offsets))
            addAll(adhanCandidatesForDate(today.plusDays(1), snapshot, offsets))
        }
            .filter { it.dateTime.isAfter(now) }
            .sortedBy { it.dateTime }

        val next = candidates.firstOrNull() ?: return null
        val seconds = Duration.between(now, next.dateTime).seconds.coerceAtLeast(0L)
        val label = snapshot.label ?: selectedLocation?.lokasi ?: DEFAULT_LOCATION_LABEL
        return PrayerTimeInfo(
            name = next.name,
            time = next.time,
            countdown = formatCountdown(seconds),
            locationName = "$label - perkiraan offline",
            isEstimated = true
        )
    }

    private fun kemenagCandidatesForDate(date: LocalDate): List<PrayerCandidate> {
        val entry = scheduleCache.scheduleEntry(date) ?: return emptyList()
        return kemenagPrayerPairs(entry).mapNotNull { (name, time) ->
            parsePrayerTime(date, time)?.let { dateTime ->
                PrayerCandidate(name = name, time = time.take(5), dateTime = dateTime)
            }
        }
    }

    private fun adhanCandidatesForDate(
        date: LocalDate,
        snapshot: PrayerCoordinateSnapshot,
        offsets: Map<String, Long>
    ): List<PrayerCandidate> {
        val rawTimes = adhanRawTimes(date, snapshot.latitude, snapshot.longitude)
        return rawTimes.map { candidate ->
            val adjusted = candidate.dateTime.plusMinutes(offsets[candidate.name] ?: 0L)
            candidate.copy(dateTime = adjusted, time = adjusted.toLocalTime().format(TIME_FORMATTER))
        }
    }

    private fun calibrationOffsets(snapshot: PrayerCoordinateSnapshot): Map<String, Long> {
        val date = LocalDate.now(zoneId)
        val entry = scheduleCache.scheduleEntry(date) ?: return emptyMap()
        val rawTimes = adhanRawTimes(date, snapshot.latitude, snapshot.longitude).associateBy { it.name }
        return kemenagPrayerPairs(entry).mapNotNull { (name, time) ->
            val kemenagTime = parsePrayerTime(date, time) ?: return@mapNotNull null
            val rawTime = rawTimes[name]?.dateTime ?: return@mapNotNull null
            name to Duration.between(rawTime, kemenagTime).toMinutes().coerceIn(-30L, 30L)
        }.toMap()
    }

    private fun adhanRawTimes(date: LocalDate, latitude: Double, longitude: Double): List<PrayerCandidate> {
        val coordinates = Coordinates(latitude, longitude)
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)
        val params = CalculationMethod.KARACHI.parameters.copy(madhab = Madhab.SHAFI)
        val prayerTimes = PrayerTimes(coordinates, dateComponents, params)
        return listOf(
            "Subuh" to prayerTimes.fajr,
            "Dzuhur" to prayerTimes.dhuhr,
            "Ashar" to prayerTimes.asr,
            "Maghrib" to prayerTimes.maghrib,
            "Isya" to prayerTimes.isha
        ).map { (name, instant) ->
            val dateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(instant.epochSeconds, instant.nanosecondsOfSecond.toLong()),
                zoneId
            )
            PrayerCandidate(name = name, time = dateTime.toLocalTime().format(TIME_FORMATTER), dateTime = dateTime)
        }
    }

    private fun kemenagPrayerPairs(entry: PrayerScheduleEntry): List<Pair<String, String>> =
        listOf(
            "Subuh" to entry.subuh,
            "Dzuhur" to entry.dzuhur,
            "Ashar" to entry.ashar,
            "Maghrib" to entry.maghrib,
            "Isya" to entry.isya
        )

    private fun parsePrayerTime(date: LocalDate, value: String): LocalDateTime? {
        val cleanValue = value.trim().take(5)
        if (cleanValue.length < 5) return null
        return runCatching {
            LocalDateTime.of(date, LocalTime.parse(cleanValue, TIME_FORMATTER))
        }.getOrNull()
    }

    private fun Map<String, PrayerScheduleData>.scheduleEntry(date: LocalDate): PrayerScheduleEntry? {
        val dateKey = date.toString()
        return values.firstNotNullOfOrNull { data ->
            data.jadwal[dateKey]
                ?: data.jadwal.values.firstOrNull { entry -> entry.matchesDate(dateKey) }
        }
    }

    private fun Map<String, PrayerScheduleData>.hasDate(date: LocalDate): Boolean =
        scheduleEntry(date) != null

    private fun PrayerScheduleEntry.matchesDate(dateKey: String): Boolean {
        if (tanggal.isBlank()) return false
        val normalized = tanggal.trim().lowercase(Locale("id", "ID"))
        return normalized == dateKey || normalized.contains(dateKey)
    }

    private fun fallbackPrayerInfo(): PrayerTimeInfo =
        PrayerTimeInfo("Subuh", "--:--", "00:00:00", DEFAULT_LOCATION_LABEL)

    private fun formatCountdown(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    private data class LocationResolution(
        val location: PrayerLocation?,
        val useEstimatedOfflineLocation: Boolean
    )

    private data class PrayerCandidate(
        val name: String,
        val time: String,
        val dateTime: LocalDateTime
    )

    private companion object {
        const val DEFAULT_LOCATION_KEYWORD = "Tasikmalaya"
        const val DEFAULT_LOCATION_LABEL = "Tasikmalaya, Indonesia"
        const val MOVEMENT_THRESHOLD_METERS = 25_000f
        val REFRESH_INTERVAL_MILLIS: Long = 6 * 60 * 60 * 1000L
        val LOCATION_CHECK_INTERVAL_MILLIS: Long = 5 * 60 * 1000L
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
