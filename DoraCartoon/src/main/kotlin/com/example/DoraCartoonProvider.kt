package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.nodes.Element

class DoraCartoonProvider : MainAPI() {
    override var mainUrl = "https://dorabash.in"
    override var name = "DoraCartoon"
    override val supportedTypes = setOf(TvType.Cartoon, TvType.AnimeMovie)
    override var lang = "hi"
    override val hasMainPage = true
    override val hasDownloadSupport = true

    override val mainPage = mainPageOf(
        "$mainUrl/anime-type/tv/page/" to "Seasons",
        "$mainUrl/anime-type/movie/page/" to "Movies",
        "$mainUrl/genre/action/page/" to "Action",
        "$mainUrl/genre/adventure/page/" to "Adventure",
        "$mainUrl/genre/comedy/page/" to "Comedy",
        "$mainUrl/genre/sci-fi/page/" to "Sci-Fi",
        "$mainUrl/genre/fantasy/page/" to "Fantasy",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = request.data + page
        val doc = app.get(url).document

        val items = doc.select("div.listupd article, div.bs, div.bsx, .film_list-wrap .flw-item, .listupd .bs").mapNotNull { el ->
            el.toSearchResult()
        }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = items,
                isHorizontalImages = true
            ),
            hasNext = items.isNotEmpty()
        )
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val anchor = this.selectFirst("a[href]") ?: return null
        val href = anchor.attr("href")
        if (href.isBlank()) return null

        val title = this.selectFirst("h2, h3, .tt, .title, .film-name")?.text()?.trim()
            ?: anchor.attr("title").trim()
        if (title.isBlank()) return null

        val posterUrl = this.selectFirst("img")?.let {
            it.attr("data-src").ifBlank { it.attr("src") }
        }

        val isMovie = href.contains("/anime-type/movie") ||
                this.selectFirst(".type")?.text()?.contains("Movie", ignoreCase = true) == true

        return if (isMovie) {
            newMovieSearchResponse(title, href, TvType.AnimeMovie) {
                this.posterUrl = posterUrl
            }
        } else {
            newAnimeSearchResponse(title, href, TvType.Cartoon) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/?s=$query").document
        return doc.select("div.listupd article, div.bs, div.bsx, .film_list-wrap .flw-item, .listupd .bs").mapNotNull { el ->
            el.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document

        val title = doc.selectFirst("h1.entry-title, h1, .anime-title")?.text()?.trim() ?: "Doraemon"
        val poster = doc.selectFirst(".thumb img, .poster img, .anime-poster img")?.let {
            it.attr("data-src").ifBlank { it.attr("src") }
        }
        val description = doc.selectFirst(".synp .shorting, .synopsis p, .entry-content p, .desc, .description")?.text()?.trim()
        val genres = doc.select(".genxed a, .genre a, .genres a").map { it.text() }

        // Check if it's a movie (single episode) or series
        val episodes = doc.select(".eplister ul li a, .episodelist ul li a, .episode-list a, ul.episodios li a").mapNotNull { ep ->
            val epHref = ep.attr("href")
            if (epHref.isBlank()) return@mapNotNull null

            val epTitle = ep.selectFirst(".epl-title, .episodiotitle, .ep-title, span")?.text()?.trim()
                ?: ep.text().trim()

            // Try to extract episode number from URL or text
            val epNum = Regex("""episode-?(\d+)""", RegexOption.IGNORE_CASE).find(epHref)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""(\d+)""").find(epTitle)?.groupValues?.get(1)?.toIntOrNull()

            newEpisode(epHref) {
                this.name = epTitle
                this.episode = epNum
            }
        }.reversed()

        return if (episodes.size <= 1 && url.contains("movie")) {
            newMovieLoadResponse(
                name = title,
                url = url,
                type = TvType.AnimeMovie,
                dataUrl = if (episodes.isNotEmpty()) episodes.first().data else url
            ) {
                this.posterUrl = poster
                this.plot = description
                this.tags = genres
            }
        } else {
            newTvSeriesLoadResponse(
                name = title,
                url = url,
                type = TvType.Cartoon,
                episodes = episodes
            ) {
                this.posterUrl = poster
                this.plot = description
                this.tags = genres
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (com.lagradost.cloudstream3.utils.ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document

        // Try to find iframe sources (common pattern for Kiranime theme)
        val iframes = doc.select("iframe[src], iframe[data-src]")
        for (iframe in iframes) {
            val iframeSrc = iframe.attr("data-src").ifBlank { iframe.attr("src") }
            if (iframeSrc.isNotBlank()) {
                // Fetch the iframe page and look for video sources inside
                try {
                    val iframeDoc = app.get(iframeSrc, referer = data).document
                    val iframeVideos = iframeDoc.select("video source[src], video[src]")
                    for (v in iframeVideos) {
                        val vSrc = v.attr("src")
                        if (vSrc.isNotBlank()) {
                            callback.invoke(
                                com.lagradost.cloudstream3.utils.newExtractorLink(
                                    source = this.name,
                                    name = this.name,
                                    url = vSrc,
                                ) {
                                    this.quality = Qualities.Unknown.value
                                }
                            )
                        }
                    }
                    // Also check for m3u8/mp4 in iframe scripts
                    val iframeScripts = iframeDoc.select("script")
                    for (s in iframeScripts) {
                        val sData = s.data()
                        val urlRegex2 = Regex("""["']([^"']+\.(?:m3u8|mp4)[^"']*)["']""")
                        urlRegex2.findAll(sData).forEach { match ->
                            val vUrl = match.groupValues[1]
                            if (vUrl.startsWith("http")) {
                                callback.invoke(
                                    com.lagradost.cloudstream3.utils.newExtractorLink(
                                        source = this.name,
                                        name = this.name,
                                        url = vUrl,
                                    ) {
                                        this.quality = Qualities.Unknown.value
                                    }
                                )
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
        }

        // Try to find direct video sources
        val videoSources = doc.select("video source[src], video[src]")
        for (source in videoSources) {
            val src = source.attr("src")
            if (src.isNotBlank()) {
                callback.invoke(
                    com.lagradost.cloudstream3.utils.newExtractorLink(
                        source = this.name,
                        name = this.name,
                        url = src,
                    ) {
                        this.quality = Qualities.Unknown.value
                    }
                )
            }
        }

        // Try to find video URLs from script tags (some sites embed URLs in JS)
        val scripts = doc.select("script:containsData(player), script:containsData(sources), script:containsData(file)")
        for (script in scripts) {
            val scriptData = script.data()

            // Look for m3u8 or mp4 URLs in the script
            val urlRegex = Regex("""(?:file|src|url|source)\s*[:=]\s*["']([^"']+\.(?:m3u8|mp4)[^"']*)["']""")
            urlRegex.findAll(scriptData).forEach { match ->
                val videoUrl = match.groupValues[1]
                if (videoUrl.isNotBlank()) {
                    callback.invoke(
                        com.lagradost.cloudstream3.utils.newExtractorLink(
                            source = this.name,
                            name = this.name,
                            url = videoUrl,
                        ) {
                            this.quality = Qualities.Unknown.value
                        }
                    )
                }
            }
        }

        return true
    }
}
