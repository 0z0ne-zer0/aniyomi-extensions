package eu.kanade.tachiyomi.animeextension.ru.anilibria.filter

import eu.kanade.tachiyomi.network.GET
import okhttp3.Headers
import okhttp3.OkHttpClient

class Requester(
    private val baseUrl: String,
    private val requestUrl: String,
    private val client: OkHttpClient,
    private val apiHeaders: Headers,
) : Runnable {
    private var response = ""

    override fun run() {
        response = client.newCall(GET("$baseUrl/$requestUrl", apiHeaders))
            .execute()
            .use { it.body.string() }
    }

    public fun getValue(): String {
        return response
    }
}
