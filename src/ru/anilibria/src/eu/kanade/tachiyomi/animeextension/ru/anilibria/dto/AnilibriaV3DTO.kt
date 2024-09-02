package eu.kanade.tachiyomi.animeextension.ru.anilibria.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TitleList(
    @SerialName("list") var list: ArrayList<SingleTitle> = arrayListOf(),
    @SerialName("pagination") var pagination: Pagination,
)

@Serializable
data class Pagination(
    @SerialName("pages") var pages: Int,
    @SerialName("current_page") var currentPage: Int,
    @SerialName("items_per_page") var itemsPerPage: Int,
    @SerialName("total_items") var totalItems: Int,
)

@Serializable
data class SingleTitle(
    @SerialName("id") var id: Int? = null,
    @SerialName("code") var code: String? = null,
    @SerialName("names") var names: Names? = Names(),
    @SerialName("franchises") var franchises: ArrayList<Franchises> = arrayListOf(),
    @SerialName("announce") var announce: String? = null,
    @SerialName("status") var status: Status,
    @SerialName("posters") var posters: Posters? = Posters(),
    @SerialName("updated") var updated: Int? = null,
    @SerialName("last_change") var lastChange: Int? = null,
    @SerialName("type") var type: AnimeType,
    @SerialName("genres") var genres: ArrayList<String> = arrayListOf(),
    @SerialName("team") var team: Team,
    @SerialName("season") var season: Season,
    @SerialName("description") var description: String? = null,
    @SerialName("in_favorites") var inFavorites: Int? = null,
    @SerialName("blocked") var blocked: Blocked? = Blocked(),
    @SerialName("player") var player: Player,
)

@Serializable
data class Names(
    @SerialName("ru") var ru: String? = null,
    @SerialName("en") var en: String? = null,
)

@Serializable
data class Franchise(
    @SerialName("id") var id: String? = null,
    @SerialName("name") var name: String? = null,
)

@Serializable
data class Releases(
    @SerialName("id") var id: Int? = null,
    @SerialName("code") var code: String? = null,
    @SerialName("ordinal") var ordinal: Int? = null,
    @SerialName("names") var names: Names? = Names(),
)

@Serializable
data class Franchises(
    @SerialName("franchise") var franchise: Franchise? = Franchise(),
    @SerialName("releases") var releases: ArrayList<Releases> = arrayListOf(),
)

@Serializable
data class Status(
    @SerialName("string") var string: String? = null,
    @SerialName("code") var code: Int? = null,
)

@Serializable
data class Thumbnail(
    @SerialName("url") var url: String? = null,
    @SerialName("raw_base64_file") var rawBase64File: String? = null,
)

@Serializable
data class Posters(
    @SerialName("small") var small: Thumbnail? = Thumbnail(),
    @SerialName("medium") var medium: Thumbnail? = Thumbnail(),
    @SerialName("original") var original: Thumbnail? = Thumbnail(),
)

@Serializable
data class AnimeType(
    @SerialName("code") var code: Int? = null,
    @SerialName("string") var string: String? = null,
    @SerialName("episodes") var episodes: Int? = null,
    @SerialName("length") var length: Int? = null,
)

@Serializable
data class Team(
    @SerialName("voice") var voice: ArrayList<String> = arrayListOf(),
    @SerialName("translator") var translator: ArrayList<String> = arrayListOf(),
    @SerialName("editing") var editing: ArrayList<String> = arrayListOf(),
    @SerialName("decor") var decor: ArrayList<String> = arrayListOf(),
    @SerialName("timing") var timing: ArrayList<String> = arrayListOf(),
)

@Serializable
data class Season(
    @SerialName("string") var string: String? = null,
    @SerialName("code") var code: Int,
    @SerialName("year") var year: Int,
)

@Serializable
data class Blocked(
    @SerialName("copyrights") var copyrights: Boolean? = null,
    @SerialName("geoip") var geoip: Boolean? = null,
    @SerialName("geoip_list") var geoipList: ArrayList<String>? = arrayListOf(),
)

@Serializable
data class Episodes(
    @SerialName("first") var first: Int? = null,
    @SerialName("last") var last: Int? = null,
)

@Serializable
data class Skips(
    @SerialName("opening") var opening: ArrayList<Int> = arrayListOf(),
    @SerialName("ending") var ending: ArrayList<Int> = arrayListOf(),
)

@Serializable
data class Hls(
    @SerialName("fhd") var fhd: String? = null,
    @SerialName("hd") var hd: String? = null,
    @SerialName("sd") var sd: String? = null,
)

@Serializable
data class Player(
    @SerialName("alternative_player") var alternativePlayer: String? = null,
    @SerialName("host") var host: String? = null,
    @SerialName("is_rutube") var isRutube: Boolean? = null,
    @SerialName("episodes") var episodes: Episodes? = Episodes(),
    @SerialName("list") var list: ArrayList<Episode> = arrayListOf(),
)

@Serializable
data class Episode(
    @SerialName("episode") var episode: String,
    @SerialName("name") var name: String? = null,
    @SerialName("uuid") var uuid: String,
    @SerialName("created_timestamp") var createdTimestamp: Long,
    @SerialName("preview") var preview: String? = null,
    @SerialName("skips") var skips: Skips? = Skips(),
    @SerialName("hls") var hls: Hls? = Hls(),
)

@Serializable
data class Quality(
    @SerialName("string") var string: String? = null,
    @SerialName("type") var type: String? = null,
    @SerialName("resolution") var resolution: String? = null,
    @SerialName("encoder") var encoder: String? = null,
    @SerialName("lq_audio") var lqAudio: String? = null,
)

@Serializable data class FilteredEpisodeList(@SerialName("player") var player: FilteredEpisodePlayer)

@Serializable
data class FilteredEpisodePlayer(
    @SerialName("list") var list: ArrayList<Episode?> = arrayListOf(),
    @SerialName("host") var host: String? = null,
)

@Serializable
data class TeamFilter(
    @SerialName("voice") var voice: ArrayList<String> = arrayListOf(),
    @SerialName("translator") var translator: ArrayList<String> = arrayListOf(),
    @SerialName("editing") var editing: ArrayList<String> = arrayListOf(),
    @SerialName("decor") var decor: ArrayList<String> = arrayListOf(),
    @SerialName("timing") var timing: ArrayList<String> = arrayListOf(),
)
