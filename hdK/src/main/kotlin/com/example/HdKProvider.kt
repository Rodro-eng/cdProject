package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.nodes.Element

class HdKProvider : MainAPI() {
    override var mainUrl = "https://tellyhd.help"
    override var name = "hdK"
    override val supportedTypes = setOf(TvType.Movie, TvType.NSFW)
    override var lang = "hi"
    override val hasMainPage = true
    override val hasDownloadSupport = true

    override val mainPage = mainPageOf(
        "$mainUrl/genre/indian/page/" to "Indian",
        "$mainUrl/genre/hindi/page/" to "Hindi",
        "$mainUrl/genre/english/page/" to "English",
        "$mainUrl/genre/uncensored/page/" to "Uncensored",
        "$mainUrl/genre/jav/page/" to "JAV",
        "$mainUrl/genre/usa/page/" to "USA",
        "$mainUrl/genre/drama/page/" to "Drama",
        "$mainUrl/genre/romance/page/" to "Romance",
        "$mainUrl/genre/comedy/page/" to "Comedy",
        "$mainUrl/genre/movies/page/" to "Movies",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = request.data + page
        val doc = app.get(url).document

        val items = doc.select("div.items article.item, #archive-content article.item").mapNotNull { el ->
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
        val anchor = this.selectFirst("div.data h3 a, h3 a, a[href]") ?: return null
        val href = anchor.attr("href")
        if (href.isBlank() || !href.contains("/movies/")) return null

        val title = anchor.text().trim()
        if (title.isBlank()) return null

        val posterUrl = this.selectFirst("div.poster img, img")?.let {
            it.attr("data-src").ifBlank { it.attr("src") }
        }

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/?s=$query").document
        return doc.select("div.result-item, div.items article.item, #archive-content article.item").mapNotNull { el ->
            val anchor = el.selectFirst("div.title a, div.data h3 a, h3 a, a[href]") ?: return@mapNotNull null
            val href = anchor.attr("href")
            if (href.isBlank()) return@mapNotNull null

            val title = anchor.text().trim()
            if (title.isBlank()) return@mapNotNull null

            val posterUrl = el.selectFirst("div.image img, div.poster img, img")?.let {
                it.attr("data-src").ifBlank { it.attr("src") }
            }

            newMovieSearchResponse(title, href, TvType.NSFW) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document

        val title = doc.selectFirst("div.data h1, h1")?.text()?.trim() ?: "Unknown"
        val poster = doc.selectFirst("div.poster img")?.let {
            it.attr("data-src").ifBlank { it.attr("src") }
        }
        val description = doc.selectFirst("div#info div.wp-content p")?.text()?.trim()
        val genres = doc.select("div.sgeneros a").map { it.text() }
        val year = doc.selectFirst("span.date")?.text()?.let {
            Regex("""(\d{4})""").find(it)?.groupValues?.get(1)?.toIntOrNull()
        }

        // Get post ID for player API
        val postId = doc.selectFirst("meta#dooplay-ajax-counter")?.attr("data-postid")
            ?: Regex("""data-post='(\d+)'""").find(doc.html())?.groupValues?.get(1)

        // Build data URL with post ID for loadLinks
        val dataUrl = if (postId != null) "$url|||$postId" else url

        return newMovieLoadResponse(
            name = title,
            url = url,
            type = TvType.NSFW,
            dataUrl = dataUrl
        ) {
            this.posterUrl = poster
            this.plot = description
            this.tags = genres
            this.year = year
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (com.lagradost.cloudstream3.utils.ExtractorLink) -> Unit
    ): Boolean {
        val parts = data.split("|||")
        val url = parts[0]
        val postId = parts.getOrNull(1)

        // Method 1: Use DooPlay player API to get iframe URLs
        if (postId != null) {
            for (i in 1..5) {
                try {
                    val apiUrl = "$mainUrl/wp-json/dooplayer/v2/$postId/movie/$i"
                    val response = app.get(apiUrl).text
                    // The API returns JSON with an embed_url field
                    val embedUrl = Regex(""""embed_url"\s*:\s*"([^"]+)"""").find(response)?.groupValues?.get(1)
                        ?.replace("\\/", "/")
                    if (embedUrl != null && embedUrl.isNotBlank() && embedUrl != "null") {
                        // Try to extract video from the embed page
                        extractVideoFromEmbed(embedUrl, url, callback)
                    }
                } catch (_: Exception) { }
            }
        }

        // Method 2: Parse the page directly for download links and iframes
        val doc = app.get(url).document

        // Check for direct download links in the content
        doc.select("a[href*=cdntelly], a[href*=playmogo], a.dipesh, button.dipesh").forEach { btn ->
            val parent = btn.parent()
            val dlUrl = parent?.attr("href") ?: btn.attr("href")
            if (dlUrl.isNotBlank() && dlUrl.startsWith("http")) {
                extractVideoFromEmbed(dlUrl, url, callback)
            }
        }

        // Check for iframes
        doc.select("iframe[src], iframe[data-src]").forEach { iframe ->
            val iframeSrc = iframe.attr("data-src").ifBlank { iframe.attr("src") }
            if (iframeSrc.isNotBlank()) {
                extractVideoFromEmbed(iframeSrc, url, callback)
            }
        }

        return true
    }

    private suspend fun extractVideoFromEmbed(
        embedUrl: String,
        referer: String,
        callback: (com.lagradost.cloudstream3.utils.ExtractorLink) -> Unit
    ) {
        try {
            val doc = app.get(embedUrl, referer = referer).document

            // Look for direct video sources
            doc.select("video source[src], video[src]").forEach { source ->
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

            // Look for video URLs in scripts (m3u8, mp4)
            doc.select("script").forEach { script ->
                val scriptData = script.data()
                val urlRegex = Regex("""["']([^"']+\.(?:m3u8|mp4)[^"']*)["']""")
                urlRegex.findAll(scriptData).forEach { match ->
                    val videoUrl = match.groupValues[1]
                    if (videoUrl.startsWith("http")) {
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
        } catch (_: Exception) { }
    }
}
