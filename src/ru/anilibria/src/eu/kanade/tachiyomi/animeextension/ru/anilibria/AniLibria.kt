package eu.kanade.tachiyomi.animeextension.ru.anilibria

import android.app.Application
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.animeextension.ru.anilibria.dto.FilteredEpisodeList
import eu.kanade.tachiyomi.animeextension.ru.anilibria.dto.SingleTitle
import eu.kanade.tachiyomi.animeextension.ru.anilibria.dto.TitleList
import eu.kanade.tachiyomi.animeextension.ru.anilibria.filter.AniLibriaTVApiV3Filter
import eu.kanade.tachiyomi.animesource.ConfigurableAnimeSource
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class AniLibria : ConfigurableAnimeSource, AnimeHttpSource() {

    override val name: String = "AnilibriaTV"

    override val baseUrl: String = "https://anilibria.tv"

    private val apiUrl: String = "https://api.anilibria.tv/v3"

    override val lang: String = "ru"

    override val supportsLatest: Boolean = true

    private val apiHeaders: Headers = Headers.Builder()
        .add("User-Agent", "Aniyomi Anilibria extension v1.1")
        .add("Accept", "application/json")
        .add("Charset", "UTF-8")
        .build()

    private val defaultRemoveFilter: String = "torrents,player.rutube,names.alternative,type.full_string,season.week_day"

    private val defaultSearchFilter: String = "names,posters,id"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val preferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    companion object {
        private const val PREF_QUALITY_KEY = "preferred_quality"
        private const val PREF_QUALITY_TITLE = "Качество по умолчанию"
        private const val PREF_QUALITY_DEFAULT = "480p"
        private val PREF_QUALITY_ENTRIES = arrayOf("1080p", "720p", "480p")
        private val PREF_QUALITY_VALUES by lazy {
            PREF_QUALITY_ENTRIES.map { it.substringBefore("p") }.toTypedArray()
        }

        const val PREFIX_SEARCH = "prefix_path:"
    }

    private fun titleListParse(response: Response): AnimesPage {
        val animeList = mutableListOf<SAnime>()
        val responseJson = json.decodeFromString<TitleList>(response.body.string())
        responseJson.list.forEach {
            val anime = SAnime.create()
            anime.title = it.names?.ru ?: it.names?.en ?: "Неизвестно"
            // if it.posters?.small?.url
            if (it.posters?.small?.url != null) {
                anime.thumbnail_url = baseUrl + it.posters?.medium?.url
            }
            anime.url = "/title?id=${it.id}&playlist_type=array&remove=$defaultRemoveFilter"
            animeList.add(anime)
        }
        val hasNextPage = responseJson.pagination.currentPage < responseJson.pagination.pages
        return AnimesPage(animeList, hasNextPage)
    }

    // ============================== Details ==============================

    private fun buildDescription(details: SingleTitle): String {
        val description = StringBuilder()
        if (details.blocked?.geoip == true) {
            description.append("🛑 ДАННОЕ АНИМЕ ЗАБЛОКИРОВАНО НА ТЕРРИТОРИИ: ${ details.blocked?.geoipList?.joinToString(", ") } 🛑\n")
        }
        if (details.blocked?.copyrights == true) {
            description.append("🛑 ДАННОЕ АНИМЕ ЗАБЛОКИРОВАНО В СВЯЗИ С КОПИРАЙТАМИ 🛑\n")
        }
        if (details.announce != null) {
            description.append("🛑 ${ details.announce } 🛑\n")
        }
        description.append("Год: ${details.season.year}\n")
        description.append("Тип: ${details.type.string}\n")
        description.append("Голоса: ${details.team.voice.joinToString(", ")}\n")
        if (details.description != null) {
            description.append("\n\n${ details.description ?: "" }")
        }
        return description.toString()
    }

    override fun animeDetailsParse(response: Response): SAnime {
        val details = json.decodeFromString<SingleTitle>(response.body.string())
        return SAnime.create().apply {
            url = baseUrl + "/release/" + details.code + ".html"
            title = details.names?.ru ?: details.names?.en ?: "Неизвестно"
            description = buildDescription(details)
            thumbnail_url = baseUrl + details.posters?.small?.url
            genre = details.genres.joinToString(", ")
            status =
                when (details.status.code) {
                    1 -> SAnime.ONGOING
                    2 -> SAnime.COMPLETED
                    3 -> SAnime.CANCELLED
                    4 -> SAnime.ON_HIATUS
                    else -> SAnime.UNKNOWN
                }
        }
    }

    // ============================== Episodes =============================

    override fun episodeListParse(response: Response): List<SEpisode> {
        val details = json.decodeFromString<SingleTitle>(response.body.string())
        val episodes = mutableListOf<SEpisode>()
        Log.d("episodeListParse", "------------------------")
        Log.d(
            "episodeListParse",
            "Title: ${details.names?.ru ?: details.names?.en ?: "Неизвестно"}",
        )
        Log.d("episodeListParse", "Code: ${details.code}")
        Log.d("episodeListParse", "Episodes: ${details.player.list}")
        Log.d("episodeListParse", "------------------------")

        if (details.player.list.size < 1) {
            throw Exception("У этого аниме нету озвученных серий")
        }

        details.player.list.forEach {
            episodes.add(
                SEpisode.create().apply {
                    name = "Серия " + it.episode + " - " + (it.name ?: "Без названия")
                    episode_number = it.episode.toFloat()
                    url =
                        "" +
                        response.request.url.toString().replace("playlist_type=array", "playlist_type=object") +
                        "&filter=player.list[${ it.episode }],player.host"
                    date_upload = it.createdTimestamp * 1000
                },
            )
        }
        episodes.sortByDescending { it.episode_number }
        Log.d("episodeListParse", "------------------------")
        Log.d("episodeListParse", episodes[0].url)
        Log.d("episodeListParse", "------------------------")
        return episodes
    }

    // ============================== Video ================================

    override fun videoListRequest(episode: SEpisode): Request = GET(episode.url, apiHeaders)

    override suspend fun getVideoList(episode: SEpisode): List<Video> {
        val pattern = "^\\{\"([0-9].?){1,2}:"
        val response = client.newCall(videoListRequest(episode)).await()

        Log.d("videoListParse", "------------------------")
        Log.d("videoListParse", episode.url)
        Log.d("videoListParse", "------------------------")

        val responseBody = response.body.string()

        val tmp = JSONObject(JSONObject(responseBody)["player"].toString())["list"]
            .toString()

        val hostAddr = Regex("\"host\":\".*\"").find(responseBody)?.value
        var out = Regex(pattern).replaceFirst(tmp, "")

        if (out[0] == '{') {
            out = "[$out]"
        }

        val jsonOutput = "{\"player\":{\"list\": $out, $hostAddr}}"

        Log.d("videoListParse", "------------------------")
        Log.d("videoListParse", jsonOutput)
        Log.d("videoListParse", "------------------------")

        val videoJson = json.decodeFromString<FilteredEpisodeList>(jsonOutput)
        val videoList = mutableListOf<Video>()
        val host = "https://" + videoJson.player.host

        Log.d("videoListParse", "------------------------")
        Log.d("videoListParse", "$videoJson")
        Log.d("videoListParse", "------------------------")

        videoJson.player.list.forEach {
            if (it != null) {
                val fhd = host + (it.hls?.fhd ?: "")
                val hd = host + (it.hls?.hd ?: "")
                val sd = host + (it.hls?.sd ?: "")
                Log.d("videoListParse", "------------------------")
                Log.d("videoListParse", "FHD: $fhd")
                Log.d("videoListParse", "HD: $hd")
                Log.d("videoListParse", "SD: $sd")
                Log.d("videoListParse", "------------------------")
                if (it.hls?.fhd != null) {
                    videoList.add(Video(fhd, "1080p", fhd))
                }
                if (it.hls?.hd != null) {
                    videoList.add(Video(hd, "720p", hd))
                }
                if (it.hls?.sd != null) {
                    videoList.add(Video(sd, "480p", sd))
                }
            }
        }

        return videoList
    }

    override fun List<Video>.sort(): List<Video> {
        val quality = preferences.getString(PREF_QUALITY_KEY, PREF_QUALITY_DEFAULT)!!
        return sortedWith(compareBy { it.quality.contains(quality) }).reversed()
    }

    // ============================== Latest ===============================

    override fun latestUpdatesParse(response: Response): AnimesPage = titleListParse(response)

    override fun latestUpdatesRequest(page: Int): Request = GET("$apiUrl/title/updates?remove=$defaultRemoveFilter&page=$page&items_per_page=8&playlist_type=array", apiHeaders)

    // ============================== Popular ==============================

    override fun popularAnimeParse(response: Response): AnimesPage = titleListParse(response)

    override fun popularAnimeRequest(page: Int): Request =
        GET(
            "$apiUrl/title/search/advanced?query={status.code}>0&remove=$defaultRemoveFilter&order_by=in_favorites&sort_direction=1&page=$page&items_per_page=8&playlist_type=array",
            apiHeaders,
        )

    // ============================== Search ===============================

    private val customFilters = AniLibriaTVApiV3Filter(apiUrl, client, apiHeaders)

    override fun getFilterList() = customFilters.getFilterList()

    // private class TitleFilter : AnimeFilter.Text("Название", "")

    override fun searchAnimeParse(response: Response): AnimesPage = titleListParse(response)

    override suspend fun getSearchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage {
        return if (query.startsWith(PREFIX_SEARCH)) {
            Log.d("searchAnimeRequest", "Ping from link-gen")
            val title = query.removePrefix(PREFIX_SEARCH).removeSuffix(".html")
            client.newCall(GET("$apiUrl/title?code=$title&playlist_type=array"))
                .awaitSuccess()
                .use(::searchAnimeByCodeParse)
        } else {
            super.getSearchAnime(page, query, filters)
        }
    }

    private fun searchAnimeByCodeParse(response: Response): AnimesPage {
        val details = animeDetailsParse(response).apply {
            setUrlWithoutDomain(response.request.url.toString())
            initialized = true
        }

        return AnimesPage(listOf(details), false)
    }

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val url = customFilters.getSearchParameters(page, query, filters)
        Log.d("searchAnimeRequest", "------------------------")
        Log.d("searchAnimeRequest", "Page: $page")
        Log.d("searchAnimeRequest", "Query: $query")
        Log.d("searchAnimeRequest", "Url: $url")
        Log.d("searchAnimeRequest", "------------------------")
        return GET(url, apiHeaders)
    }

    // ============================== Settings ===============================

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            key = PREF_QUALITY_KEY
            title = PREF_QUALITY_TITLE
            entries = PREF_QUALITY_ENTRIES
            entryValues = PREF_QUALITY_VALUES
            setDefaultValue(PREF_QUALITY_DEFAULT)
            summary = "%s"

            setOnPreferenceChangeListener { _, newValue ->
                val selected = newValue as String
                val index = findIndexOfValue(selected)
                val entry = entryValues[index] as String
                preferences.edit().putString(key, entry).commit()
            }
        }.also(screen::addPreference)
    }
}
