package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.Qualities

class MagicIXProvider : MainAPI() {
    override var mainUrl = "http://moviemazic.xyz"
    override var name = "MagicIX"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Live)
    override var lang = "en"
    override val hasMainPage = true
    override val hasDownloadSupport = false
    override val hasSearch = true

    companion object {
        // CDN base used by OvooVideojs player
        private const val CDN_BASE = "http://cdn.moviemazic.xyz:8083"
    }

    // ─────────────────────────────────────────────────────────────
    // Main page sections
    // ─────────────────────────────────────────────────────────────
    override val mainPage = mainPageOf(
        "movies|||filter_movies" to "Latest Movies",
        "tvseries|||filter_tvseries" to "Latest TV Series",
        "livetv" to "Live TV",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val (category, endpoint) = request.data.split("|||") + listOf("")
        return when (category) {
            "movies" -> fetchFilterPage(endpoint, page, request.name, TvType.Movie)
            "tvseries" -> fetchFilterPage(endpoint, page, request.name, TvType.TvSeries)
            else -> fetchLiveTvPage(request.name)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers: parse product cards from JSON response
    // ─────────────────────────────────────────────────────────────
    private fun parseProductCards(html: String, tvType: TvType): List<SearchResponse> {
        val results = mutableListOf<SearchResponse>()
        val productRegex = Regex(
            """href=\\"(https?://moviemazic\.xyz/watch/[^"\\]+)\\">\s*<img[^>]+src=\\"([^"\\]+)\\"[^>]+alt=\\"([^"\\]+)\\"""
        )
        productRegex.findAll(html).forEach { m ->
            val url = m.groupValues[1].replace("\\/", "/")
            val poster = m.groupValues[2].replace("\\/", "/")
            val title = m.groupValues[3]
            results.add(
                newMovieSearchResponse(
                    name = title,
                    url = url,
                    type = tvType
                ) {
                    this.posterUrl = poster
                }
            )
        }
        return results
    }

    private suspend fun fetchFilterPage(
        endpoint: String,
        page: Int,
        sectionName: String,
        tvType: TvType
    ): HomePageResponse {
        val response = app.post(
            "$mainUrl/$endpoint/$page",
            data = mapOf("action" to "fetch_data", "sort" to "desc")
        ).text

        // Extract movie_list value from JSON
        val listMatch = Regex(""""movie_list":"(.*?)","pagination_link":""", RegexOption.DOT_MATCHES_ALL)
            .find(response)
        val rawHtml = listMatch?.groupValues?.get(1) ?: ""

        val items = parseProductCards(rawHtml, tvType)

        // Check if there's a next page from pagination
        val hasNext = response.contains("data-ci-pagination-page=\\\"${page + 1}\\\"")

        return newHomePageResponse(
            list = HomePageList(name = sectionName, list = items, isHorizontalImages = false),
            hasNext = hasNext
        )
    }

    private suspend fun fetchLiveTvPage(sectionName: String): HomePageResponse {
        val html = app.get("$mainUrl/live-tv.html").text
        val results = mutableListOf<SearchResponse>()

        val channelRegex = Regex(
            """href="(https?://moviemazic\.xyz/live-tv/[^"]+)"[^>]*>\s*<img[^>]+src="([^"]+)"[^>]+alt="([^"]+)""""
        )
        channelRegex.findAll(html).forEach { m ->
            val url = m.groupValues[1]
            val poster = m.groupValues[2]
            val title = m.groupValues[3]
            if (!url.contains("/category/")) {
                results.add(
                    newMovieSearchResponse(
                        name = title,
                        url = url,
                        type = TvType.Live
                    ) {
                        this.posterUrl = poster
                    }
                )
            }
        }

        return newHomePageResponse(
            list = HomePageList(name = sectionName, list = results, isHorizontalImages = true),
            hasNext = false
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Search
    // ─────────────────────────────────────────────────────────────
    override suspend fun search(query: String): List<SearchResponse> {
        val html = app.get("$mainUrl/search?q=${query.replace(" ", "+")}").text
        val results = mutableListOf<SearchResponse>()

        // movie/tv results use class="product mb-4"
        val productRegex = Regex(
            """<div class="product mb-4"[^>]*>[\s\S]*?href="([^"]+)"[^>]*>\s*<img[^>]+src="([^"]+)"[^>]+alt="([^"]+)"""
        )
        productRegex.findAll(html).forEach { m ->
            val url = m.groupValues[1]
            val poster = m.groupValues[2]
            val title = m.groupValues[3]
            val type = if (url.contains("-tv.html")) TvType.TvSeries else TvType.Movie
            results.add(
                newMovieSearchResponse(
                    name = title,
                    url = url,
                    type = type
                ) {
                    this.posterUrl = poster
                }
            )
        }
        return results
    }

    // ─────────────────────────────────────────────────────────────
    // Load: movie, tv series, or live tv detail page
    // ─────────────────────────────────────────────────────────────
    override suspend fun load(url: String): LoadResponse {
        val isLive = url.contains("/live-tv/")
        val isTv = url.contains("-tv.html")
        val html = app.get(url).text

        // --- Common metadata ---
        val titleRaw = Regex("""<title>(.*?)</title>""", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1)?.trim() ?: name
        val title = titleRaw.replace(" - Moviemazic", "").trim()

        val posterMatch = Regex("""uploads/poster_image/([^"?]+)""").find(html)
        val poster = if (posterMatch != null) "$mainUrl/uploads/poster_image/${posterMatch.groupValues[1]}" else null

        val plotMatch = Regex("""<div[^>]*class="[^"]*description[^"]*"[^>]*>([\s\S]*?)</div>""", RegexOption.IGNORE_CASE)
            .find(html)
        val plot = plotMatch?.groupValues?.get(1)?.replace(Regex("<[^>]+>"), "")?.trim()

        // --- Live TV ---
        if (isLive) {
            // Extract stream from OvooVideojs.mount
            val mountMatch = Regex("""OvooVideojs\.mount\((\{.*?\})\)""", RegexOption.DOT_MATCHES_ALL).find(html)
            val streamUrl = mountMatch?.groupValues?.get(1)
                ?.let { Regex(""""source":"([^"]+)"""").find(it)?.groupValues?.get(1)?.replace("\\/", "/") }
                ?: url

            return newMovieLoadResponse(
                name = title,
                url = url,
                type = TvType.Live,
                dataUrl = streamUrl
            ) {
                this.posterUrl = poster
                this.plot = plot ?: "Live TV on MagicIX"
            }
        }

        // --- TV Series ---
        if (isTv) {
            // Parse episodes: each ep is a link with ?key=xxx
            val episodes = mutableListOf<Episode>()

            // Seasons are shown as headers, episodes as cards with links
            val epRegex = Regex(
                """href="(${Regex.escape(url)}[^"]*key=[^"]+)"[^>]*>[\s\S]*?alt="([^"]+)""""
            )
            var epNum = 1
            epRegex.findAll(html).forEach { m ->
                val epUrl = m.groupValues[1]
                val epTitle = m.groupValues[2]
                val posterEpMatch = Regex("""src="([^"]+episodes/[^"]+)"""").find(m.value)

                episodes.add(
                    newEpisode(epUrl) {
                        this.name = epTitle
                        this.episode = epNum++
                        this.season = 1
                        this.posterUrl = posterEpMatch?.groupValues?.get(1)
                    }
                )
            }

            return newTvSeriesLoadResponse(
                name = title,
                url = url,
                type = TvType.TvSeries,
                episodes = episodes
            ) {
                this.posterUrl = poster
                this.plot = plot
            }
        }

        // --- Movie ---
        // Get the default stream URL from OvooVideojs or the first Server# link
        val mountMatch = Regex("""OvooVideojs\.mount\((\{.*?\})\)""", RegexOption.DOT_MATCHES_ALL).find(html)
        val streamUrl = mountMatch?.groupValues?.get(1)
            ?.let { Regex(""""source":"([^"]+)"""").find(it)?.groupValues?.get(1)?.replace("\\/", "/") }

        // Find alternative server links (e.g., ?key=...)
        val serverLinks = mutableListOf<String>()
        if (streamUrl != null) serverLinks.add(streamUrl)

        // Additional server keys
        Regex("""href="($mainUrl/watch/[^"]+\?key=[^"]+)"""").findAll(html).forEach {
            serverLinks.add(it.groupValues[1])
        }

        return newMovieLoadResponse(
            name = title,
            url = url,
            type = TvType.Movie,
            dataUrl = serverLinks.firstOrNull() ?: url
        ) {
            this.posterUrl = poster
            this.plot = plot
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Load Links: resolve actual stream URL
    // ─────────────────────────────────────────────────────────────
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (com.lagradost.cloudstream3.utils.ExtractorLink) -> Unit
    ): Boolean {
        // If data is already a direct m3u8 / mp4, use it immediately
        if (data.contains(".m3u8") || data.contains(".mp4")) {
            callback.invoke(
                com.lagradost.cloudstream3.utils.newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = data,
                ) {
                    this.quality = Qualities.P1080.value
                }
            )
            return true
        }

        // Otherwise it's a page URL — fetch it and extract OvooVideojs source
        try {
            val html = app.get(data).text
            val mountMatch = Regex("""OvooVideojs\.mount\((\{.*?\})\)""", RegexOption.DOT_MATCHES_ALL).find(html)
            val streamUrl = mountMatch?.groupValues?.get(1)
                ?.let { Regex(""""source":"([^"]+)"""").find(it)?.groupValues?.get(1)?.replace("\\/", "/") }

            if (!streamUrl.isNullOrBlank()) {
                val isLive = mountMatch.groupValues[1].contains("\"live\":true")
                callback.invoke(
                    com.lagradost.cloudstream3.utils.newExtractorLink(
                        source = this.name,
                        name = this.name,
                        url = streamUrl,
                    ) {
                        this.quality = if (isLive) Qualities.Unknown.value else Qualities.P1080.value
                    }
                )
                return true
            }
        } catch (_: Throwable) {}

        return false
    }
}
