package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.weather.WeatherForecastResponse
import com.alhasanah.alhasanahmedia.data.model.weather.WeatherAlertItem
import com.alhasanah.alhasanahmedia.data.remote.weather.WeatherApiService
import java.io.StringReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class WeatherRepositoryImpl(
    private val apiService: WeatherApiService
) : WeatherRepository {
    override fun getForecast(adm4: String): Flow<Result<WeatherForecastResponse>> = flow {
        try {
            emit(Result.success(apiService.getForecast(adm4 = adm4)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getNowcastAlerts(): Flow<Result<List<WeatherAlertItem>>> = flow {
        try {
            val xml = apiService.getNowcastAlerts().string()
            emit(Result.success(parseRssAlerts(xml)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    private fun parseRssAlerts(xml: String): List<WeatherAlertItem> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))
        val alerts = mutableListOf<WeatherAlertItem>()
        var eventType = parser.eventType
        var insideItem = false
        var currentTag = ""
        var title = ""
        var description = ""
        var pubDate = ""
        var link = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name.orEmpty()
                    if (currentTag == "item") {
                        insideItem = true
                        title = ""
                        description = ""
                        pubDate = ""
                        link = ""
                    }
                }
                XmlPullParser.TEXT -> if (insideItem) {
                    val text = parser.text.orEmpty().trim()
                    if (text.isNotBlank()) {
                        when (currentTag) {
                            "title" -> title += text
                            "description" -> description += text
                            "pubDate" -> pubDate += text
                            "link" -> link += text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") {
                        if (title.isNotBlank() || description.isNotBlank()) {
                            alerts += WeatherAlertItem(
                                title = title.ifBlank { "Peringatan dini cuaca" },
                                description = description,
                                pubDate = pubDate,
                                link = link
                            )
                        }
                        insideItem = false
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }
        return alerts
    }
}
