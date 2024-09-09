package eu.kanade.tachiyomi.animeextension.ru.anilibria.filter

import android.util.Log
import eu.kanade.tachiyomi.animeextension.ru.anilibria.dto.TeamFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilter.TriState
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.OkHttpClient

class AniLibriaTVApiV3Filter(
    private val baseUrl: String,
    private val client: OkHttpClient,
    private val apiHeaders: Headers,
) {
    // ============================== Base ==============================

    private val json = Json { ignoreUnknownKeys = true }

    open class TriStateFilterList(name: String, val vals: Array<String>) :
        AnimeFilter.Group<TriState>(name, vals.map(AniLibriaTVApiV3Filter::TriStateVal))
    private class TriStateVal(name: String) : TriState(name)

    open class CheckBoxFilterList(name: String, val pairs: Array<Pair<String, String>>) :
        AnimeFilter.Group<AnimeFilter.CheckBox>(name, pairs.map { CheckBoxVal(it.first, false) })
    private class CheckBoxVal(name: String, state: Boolean = false) : AnimeFilter.CheckBox(name, state)

    fun getFilterList() =
        AnimeFilterList(
            AnimeFilter.Header("Базовый поиск"),
            OrderList(orderNames),
            OrderDirectionList(orderDirectionNames),
            YearsFilter(),
            SeasonFilter(),
            GenresFilter(),
            StatusFilter(),
            TitleTypesFilter(),
            AnimeFilter.Separator(),
            AnimeFilter.Header("Команда"),
            VoiceActorsFilter(),
            TranslatorsFilter(),
            EditorsFilter(),
            DecoratorsFilter(),
            TimingFilter(),
        )

    // ============================== Title Name ==============================

    private inner class TitleFilter : AnimeFilter.Text("Название", "")

    // ============================== Years ==============================

    private inner class YearsFilter : CheckBoxFilterList("Год", yearsList)
    private val yearsList = getYearsFilter()

    private fun getYearsFilter(): Array<Pair<String, String>> {
        val func = Requester(baseUrl, "years", client, apiHeaders)
        val thread = Thread(func)
        thread.start()
        thread.join()
        val response = func.getValue()
        val res = response
            .let { json.decodeFromString<List<Int>>(it) }
            .map { Pair(it.toString(), it.toString()) }
            .reversed()

        Log.d("getYearsFilter", res.toString())
        return res.toTypedArray()
    }

    // ============================== Genres ==============================

    private inner class GenresFilter : CheckBoxFilterList("Жанр", genresList)
    private val genresList = getGenresFilter()

    private fun getGenresFilter(): Array<Pair<String, String>> {
        val func = Requester(baseUrl, "genres", client, apiHeaders)
        val thread = Thread(func)
        thread.start()
        thread.join()
        val response = func.getValue()
        val res = response
            .let { json.decodeFromString<List<String>>(it) }
            .map { Pair(it.toString().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }, it.toString()) }
            .reversed()

        Log.d("getGenresFilter", res.toString())
        return res.toTypedArray()
    }

    // ============================== Team ===============================

    private inner class VoiceActorsFilter : CheckBoxFilterList("Озвучка", teamList.voice.map { Pair(it, it) }.toTypedArray())
    private inner class TranslatorsFilter : CheckBoxFilterList("Перевод", teamList.translator.map { Pair(it, it) }.toTypedArray())
    private inner class EditorsFilter : CheckBoxFilterList("Монтаж", teamList.editing.map { Pair(it, it) }.toTypedArray())
    private inner class DecoratorsFilter : CheckBoxFilterList("Декор", teamList.decor.map { Pair(it, it) }.toTypedArray())
    private inner class TimingFilter : CheckBoxFilterList("Тайминг", teamList.timing.map { Pair(it, it) }.toTypedArray())
    private val teamList = getTeamFilter()

    private fun getTeamFilter(): TeamFilter {
        val func = Requester(baseUrl, "team", client, apiHeaders)
        val thread = Thread(func)
        thread.start()
        thread.join()
        val response = func.getValue()
        val res = response
            .let { json.decodeFromString<TeamFilter>(it) }

        Log.d("getTeamFilter", res.toString())
        return res
    }

    // ============================== Title Type ==============================

    private inner class TitleTypesFilter : CheckBoxFilterList("Тип тайтла", titleTypesList)
    private val titleTypesList = arrayOf(
        Pair("Фильм", "0"),
        Pair("TV", "1"),
        Pair("OVA", "2"),
        Pair("ONA", "3"),
        Pair("Спешл", "4"),
        Pair("WEB", "5"),
    )

    // ============================== Season ==============================

    private inner class SeasonFilter : CheckBoxFilterList("Сезон", seasonList)
    private val seasonList = arrayOf(
        Pair("Зима", "1"),
        Pair("Весна", "2"),
        Pair("Лето", "3"),
        Pair("Осень", "4"),
    )

    // ============================== Status ==============================

    private inner class StatusFilter : CheckBoxFilterList("Статус", statusList)
    private val statusList = arrayOf(
        Pair("В работе", "1"),
        Pair("Завершен", "2"),
        Pair("Скрыт", "3"),
        Pair("Неонгоинг", "4"),
    )

    // ============================== Order ==============================

    private data class Order(val name: String, val id: String)
    private class OrderList(Orders: Array<String>) : AnimeFilter.Select<String>("Порядок", Orders)
    private val orderNames = getOrder().map { it.name }.toTypedArray()
    private fun getOrder() = listOf(
        Order("Новенькое", "new"),
        Order("Популярное", "popular"),
    )

    private data class OrderDirection(val name: String, val id: String)
    private class OrderDirectionList(OrderDirections: Array<String>) : AnimeFilter.Select<String>("Направление cортировки", OrderDirections)
    private val orderDirectionNames = getOrderDirection().map { it.name }.toTypedArray()
    private fun getOrderDirection() = listOf(
        OrderDirection("По возрастанию", "ascending"),
        OrderDirection("По убыванию", "descending"),
    )

    // ============================== Filter Builder ==============================

    fun getSearchParameters(page: Int, query: String, filters: AnimeFilterList): String {
        val basestring = "$baseUrl/title/search"
        var searchStr = ""
        var orderStr = ""
        var orderDirectionStr = ""
        var yearStr = ""
        var seasonStr = ""
        var genresStr = ""
        var statusStr = ""
        var titleTypeStr = ""

        var voiceActorsStr = ""
        var translatorsStr = ""
        var editorsStr = ""
        var decoratorsStr = ""
        var timingStr = ""

        var filterStr = ""

        val modifiers = "playlist_type=array&page=$page&items_per_page=8"

        Log.d("searchAnimeRequest", "------------------------")
        filters.forEach { filter ->
            when (filter) {
                is OrderList -> { // ---Order
                    orderStr = if (getOrder()[filter.state].id == "new") {
                        "order_by=updated"
                    } else {
                        "order_by=in_favorites"
                    }
                }
                is OrderDirectionList -> {
                    orderDirectionStr = if (getOrderDirection()[filter.state].id == "ascending") {
                        "sort_direction=0"
                    } else {
                        "sort_direction=1"
                    }
                }
                is YearsFilter -> { // ---Year
                    filter.state.forEach { Year ->
                        if (Year.state) {
                            if (yearStr.isNotEmpty()) {
                                yearStr += "," + Year.name
                            } else {
                                yearStr = Year.name
                            }
                        }
                    }
                    Log.d("searchAnimeRequest", "Added year: $yearStr")
                    if (yearStr != "") {
                        filterStr += " ({season.year} in ($yearStr)) and"
                    }
                }
                is SeasonFilter -> { // ---Season
                    filter.state.forEach { Season ->
                        if (Season.state) {
                            val seasonId = seasonList.find { it.first == Season.name }?.second
                            if (seasonId != null) {
                                if (seasonStr.isNotEmpty()) {
                                    seasonStr += ",$seasonId"
                                } else {
                                    seasonStr = seasonId
                                }
                            }
                        }
                        Log.d("searchAnimeRequest", "Added season: $seasonStr")
                        if (seasonStr != "") {
                            filterStr += " ({season.code} in ($seasonStr)) and"
                        }
                    }
                }
                is GenresFilter -> { // ---Genres
                    filter.state.forEach { Genre ->
                        if (Genre.state) {
                            if (genresStr.isNotEmpty()) {
                                genresStr += "," + Genre.name
                            } else {
                                genresStr = Genre.name
                            }
                        }
                    }
                    Log.d("searchAnimeRequest", "Added genres: $genresStr")
                    if (genresStr != "") {
                        filterStr += " (($genresStr) in {genres}) and"
                    }
                }
                is StatusFilter -> { // ---Status
                    filter.state.forEach { Status ->
                        if (Status.state) {
                            val statusId = statusList.find { it.first == Status.name }?.second
                            if (statusId != null) {
                                if (statusStr.isNotEmpty()) {
                                    statusStr += ",$statusId"
                                } else {
                                    statusStr = statusId
                                }
                            }
                        }
                        Log.d("searchAnimeRequest", "Added statuses: $statusStr")
                        if (statusStr != "") {
                            filterStr += " ({status.code} in ($statusStr)) and"
                        }
                    }
                }
                is TitleTypesFilter -> { // ---Title Type
                    filter.state.forEach { TitleType ->
                        if (TitleType.state) {
                            val titleTypeId = titleTypesList.find { it.first == TitleType.name }?.second
                            if (titleTypeId != null) {
                                if (titleTypeStr.isNotEmpty()) {
                                    titleTypeStr += ",$titleTypeId"
                                } else {
                                    titleTypeStr = titleTypeId
                                }
                            }
                        }
                        Log.d("searchAnimeRequest", "Added title types: $titleTypeStr")
                        if (titleTypeStr != "") {
                            filterStr += " ({type.code} in ($titleTypeStr)) and"
                        }
                    }
                }

                // Advanced Filter

                is VoiceActorsFilter -> { // ---Voice Actors
                    filter.state.forEach { VoiceActor ->
                        if (VoiceActor.state) {
                            if (voiceActorsStr.isNotEmpty()) {
                                voiceActorsStr += "," + VoiceActor.name
                            } else {
                                voiceActorsStr = VoiceActor.name
                            }
                        }
                    }
                    Log.d("searchAnimeRequest", "Added voice actors: $voiceActorsStr")
                    if (voiceActorsStr != "") {
                        filterStr += " (($voiceActorsStr) in {team.voice}) and"
                    }
                }
                is TranslatorsFilter -> { // ---Translators
                    filter.state.forEach { Translator ->
                        if (Translator.state) {
                            if (translatorsStr.isNotEmpty()) {
                                translatorsStr += "," + Translator.name
                            } else {
                                translatorsStr = Translator.name
                            }
                        }
                    }
                    Log.d("searchAnimeRequest", "Added translators: $translatorsStr")
                    if (translatorsStr != "") {
                        filterStr += " (($translatorsStr) in {team.translator}) and"
                    }
                }
                is EditorsFilter -> { // ---Editors
                    filter.state.forEach { Editor ->
                        if (Editor.state) {
                            if (editorsStr.isNotEmpty()) {
                                editorsStr += "," + Editor.name
                            } else {
                                editorsStr = Editor.name
                            }
                        }
                    }
                    Log.d("searchAnimeRequest", "Added editors: $editorsStr")
                    if (editorsStr != "") {
                        filterStr += " (($editorsStr) in {team.editing}) and"
                    }
                }
                is DecoratorsFilter -> { // ---Decorators
                    filter.state.forEach { Decorator ->
                        if (Decorator.state) {
                            if (decoratorsStr.isNotEmpty()) {
                                decoratorsStr += "," + Decorator.name
                            } else {
                                decoratorsStr = Decorator.name
                            }
                        }
                    }
                    Log.d("searchAnimeRequest", "Added decorators: $decoratorsStr")
                    if (decoratorsStr != "") {
                        filterStr += " (($decoratorsStr) in {team.decor}) and"
                    }
                }
                is TimingFilter -> { // ---Timing
                    filter.state.forEach { Timing ->
                        if (Timing.state) {
                            if (timingStr.isNotEmpty()) {
                                timingStr += "," + Timing.name
                            } else {
                                timingStr = Timing.name
                            }
                        }
                    }
                    Log.d("searchAnimeRequest", "Added timers: $timingStr")
                    if (timingStr != "") {
                        filterStr += " (($timingStr) in {team.timing}) and"
                    }
                }

                else -> {}
            }
        }
        Log.d("searchAnimeRequest", "------------------------")

        Log.d("searchAnimeRequest", "------------------------")
        Log.d("searchAnimeRequest", "Sort query: $filterStr")
        Log.d("searchAnimeRequest", "------------------------")

        if (query.isNotEmpty()) {
            searchStr = if (filterStr != "") {
                "({names.en}~=\"$query\" or {names.ru}~=\"$query\") and"
            } else {
                "search=$query"
            }
        }

        val totalstring = if (filterStr != "") {
            Regex(" and\$").replace("$basestring/advanced?$modifiers&$orderStr&$orderDirectionStr&query=$searchStr$filterStr", "")
        } else {
            "$basestring?$searchStr&$modifiers"
        }

        return totalstring
    }
}
