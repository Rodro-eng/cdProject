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

    override val mainPage = mainPageOf(
        "movies|||filter_movies"     to "Latest Movies",
        "tvseries|||filter_tvseries" to "Latest TV Series",
        "livetv|||"                  to "Live TV",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val parts    = request.data.split("|||")
        val category = parts.getOrElse(0) { "" }
        val endpoint = parts.getOrElse(1) { "" }
        return when (category) {
            "movies"   -> fetchFilterPage(endpoint, page, request.name, TvType.Movie)
            "tvseries" -> fetchFilterPage(endpoint, page, request.name, TvType.TvSeries)
            else       -> fetchLiveTvPage(request.name)
        }
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

        val items = mutableListOf<SearchResponse>()
        val movieListRegex = Regex(
            "\"movie_list\":\"(.*?)\",\"pagination_link\":",
            RegexOption.DOT_MATCHES_ALL
        )
        val rawHtml = movieListRegex.find(response)?.groupValues?.get(1) ?: ""

        val productRegex = Regex(
            "href=\\\\\"(https?://moviemazic\\.xyz/watch/[^\"\\\\]+)\\\\\">" +
            "\\s*<img[^>]+src=\\\\\"([^\"\\\\]+)\\\\\"[^>]+alt=\\\\\"([^\"\\\\]+)\\\\\""
        )
        productRegex.findAll(rawHtml).forEach { m ->
            val itemUrl    = m.groupValues[1].replace("\\/", "/")
            val poster = m.groupValues[2].replace("\\/", "/")
            val title  = m.groupValues[3]
            items.add(newMovieSearchResponse(name = title, url = itemUrl, type = tvType) {
                this.posterUrl = poster
            })
        }

        val hasNext = response.contains("data-ci-pagination-page=\\\"${page + 1}\\\"")
        return newHomePageResponse(
            list    = HomePageList(name = sectionName, list = items, isHorizontalImages = false),
            hasNext = hasNext
        )
    }

    private suspend fun fetchLiveTvPage(sectionName: String): HomePageResponse {
        val html    = app.get("$mainUrl/live-tv.html").text
        val results = mutableListOf<SearchResponse>()

        val channelRegex = Regex(
            "href=\"(https?://moviemazic\\.xyz/live-tv/[^\"]+)\"[^>]*>" +
            "\\s*<img[^>]+src=\"([^\"]+)\"[^>]+alt=\"([^\"]+)\""
        )
        channelRegex.findAll(html).forEach { m ->
            val itemUrl = m.groupValues[1]
            val poster  = m.groupValues[2]
            val title   = m.groupValues[3]
            if (!itemUrl.contains("/category/")) {
                results.add(newMovieSearchResponse(name = title, url = itemUrl, type = TvType.Live) {
                    this.posterUrl = poster
                })
            }
        }

        return newHomePageResponse(
            list    = HomePageList(name = sectionName, list = results, isHorizontalImages = true),
            hasNext = false
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val html    = app.get("$mainUrl/search?q=${query.replace(" ", "+")}").text
        val results = mutableListOf<SearchResponse>()

        val productRegex = Regex(
            "<div class=\"product mb-4\"[^>]*>[\\s\\S]*?" +
            "href=\"([^\"]+)\"[^>]*>\\s*<img[^>]+src=\"([^\"]+)\"[^>]+alt=\"([^\"]+)\""
        )
        productRegex.findAll(html).forEach { m ->
            val itemUrl = m.groupValues[1]
            val poster  = m.groupValues[2]
            val title   = m.groupValues[3]
            val type    = if (itemUrl.contains("-tv.html")) TvType.TvSeries else TvType.Movie
            results.add(newMovieSearchResponse(name = title, url = itemUrl, type = type) {
                this.posterUrl = poster
            })
        }
        return results
    }

    override suspend fun load(url: String): LoadResponse {
        val isLive = url.contains("/live-tv/")
        val isTv   = url.contains("-tv.html")
        val html   = app.get(url).text

        val title = Regex("<title>(.*?)</title>", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1)?.trim()
            ?.replace(" - Moviemazic", "")?.trim() ?: name

        val poster = Regex("uploads/poster_image/([^\"?]+)").find(html)
            ?.let { "$mainUrl/uploads/poster_image/${it.groupValues[1]}" }

        val plot = Regex(
            "<div[^>]*class=\"[^\"]*description[^\"]*\"[^>]*>([\\s\\S]*?)</div>",
            RegexOption.IGNORE_CASE
        ).find(html)?.groupValues?.get(1)?.replace(Regex("<[^>]+>"), "")?.trim()

        val streamUrl = extractOvooSource(html)

        if (isLive) {
            return newMovieLoadResponse(
                name    = title,
                url     = url,
                type    = TvType.Live,
                dataUrl = streamUrl ?: url
            ) {
                this.posterUrl = poster
                this.plot      = plot ?: "Live TV on MagicIX"
            }
        }

        if (isTv) {
            val episodes   = mutableListOf<Episode>()
            val escapedUrl = Regex.escape(url)
            val epRegex = Regex("href=\"($escapedUrl[^\"]*key=[^\"]+)\"[^>]*>[\\s\\S]*?alt=\"([^\"]+)\"")
            var epNum = 1
            epRegex.findAll(html).forEach { m ->
                val epUrl    = m.groupValues[1]
                val epTitle  = m.groupValues[2]
                val epPoster = Regex("src=\"([^\"]+episodes/[^\"]+)\"").find(m.value)?.groupValues?.get(1)
                episodes.add(newEpisode(epUrl) {
                    this.name      = epTitle
                    this.episode   = epNum++
                    this.season    = 1
                    this.posterUrl = epPoster
                })
            }
            return newTvSeriesLoadResponse(
                name     = title,
                url      = url,
                type     = TvType.TvSeries,
                episodes = episodes
            ) {
                this.posterUrl = poster
                this.plot      = plot
            }
        }

        val serverLinks = mutableListOf<String>()
        if (streamUrl != null) serverLinks.add(streamUrl)
        Regex("href=\"($mainUrl/watch/[^\"]+\\?key=[^\"]+)\"").findAll(html).forEach {
            serverLinks.add(it.groupValues[1])
        }

        return newMovieLoadResponse(
            name    = title,
            url     = url,
            type    = TvType.Movie,
            dataUrl = serverLinks.firstOrNull() ?: url
        ) {
            this.posterUrl = poster
            this.plot      = plot
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (com.lagradost.cloudstream3.utils.ExtractorLink) -> Unit
    ): Boolean {
        if (data.contains(".m3u8") || data.contains(".mp4")) {
            callback.invoke(
                com.lagradost.cloudstream3.utils.newExtractorLink(
                    source = this.name,
                    name   = this.name,
                    url    = data,
                ) {
                    this.quality = Qualities.P1080.value
                }
            )
            return true
        }

        try {
            val html      = app.get(data).text
            val streamUrl = extractOvooSource(html)
            if (!streamUrl.isNullOrBlank()) {
                val isLiveStream = html.contains("\"live\":true")
                callback.invoke(
                    com.lagradost.cloudstream3.utils.newExtractorLink(
                        source = this.name,
                        name   = this.name,
                        url    = streamUrl,
                    ) {
                        this.quality = if (isLiveStream) Qualities.Unknown.value else Qualities.P1080.value
                    }
                )
                return true
            }
        } catch (_: Throwable) {}

        return false
    }

    private fun extractOvooSource(html: String): String? {
        val mountJson = Regex(
            "OvooVideojs\\.mount\\((\\{.*?\\})\\)",
            RegexOption.DOT_MATCHES_ALL
        ).find(html)?.groupValues?.get(1) ?: return null
        return Regex("\"source\":\"([^\"]+)\"").find(mountJson)
            ?.groupValues?.get(1)?.replace("\\/", "/")
    }
}
