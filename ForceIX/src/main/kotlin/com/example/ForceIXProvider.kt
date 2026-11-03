package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.Qualities

class ForceIXProvider : MainAPI() {
    override var mainUrl = "http://redforce.live"
    override var name = "ForceIX"
    override val supportedTypes = setOf(TvType.Live)
    override var lang = "bn"
    override val hasMainPage = true
    override val hasDownloadSupport = false

    data class ForceChannel(
        val id: String,
        val name: String,
        val category: String,
        val poster: String
    )

    override val mainPage = mainPageOf(
        "all"     to "All Channels",
        "Bangla"  to "Bangla",
        "Sports"  to "Sports",
        "Hindi"   to "Hindi",
        "English" to "English",
        "Kids"    to "Kids",
        "Islamic" to "Islamic",
    )

    private var cachedChannels: List<ForceChannel>? = null

    private suspend fun getChannels(): List<ForceChannel> {
        cachedChannels?.let { return it }

        val html = app.get("$mainUrl/").text
        val channels = mutableListOf<ForceChannel>()

        val pattern = Regex(
            "<li class=\"([^\"]+)\\s+All\">\\s*<a[^>]*onclick=\"view\\.location\\.href='player\\.php\\?stream=(\\d+)'\"[^>]*>\\s*<img\\s+src=\"([^\"]+)\"\\s+alt=\"([^\"]+)\"",
            RegexOption.DOT_MATCHES_ALL
        )

        pattern.findAll(html).forEach { match ->
            val category = match.groupValues[1].trim()
            val streamId = match.groupValues[2].trim()
            val rawImg   = match.groupValues[3].trim()
            val chName   = match.groupValues[4].trim()

            val posterUrl = if (rawImg.startsWith("http")) {
                rawImg.replace(" ", "%20")
            } else {
                "$mainUrl/${rawImg.trimStart('/')}".replace(" ", "%20")
            }

            channels.add(
                ForceChannel(
                    id       = streamId,
                    name     = chName,
                    category = category,
                    poster   = posterUrl
                )
            )
        }

        cachedChannels = channels
        return channels
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val channels = getChannels()
        val filterCategory = request.data

        val filtered = if (filterCategory.equals("all", ignoreCase = true)) {
            channels
        } else {
            channels.filter { it.category.equals(filterCategory, ignoreCase = true) }
        }

        val items = filtered.map { channel ->
            newMovieSearchResponse(
                name = channel.name,
                url  = "$mainUrl/player.php?stream=${channel.id}",
                type = TvType.Live
            ) {
                this.posterUrl = channel.poster
            }
        }

        return newHomePageResponse(
            list    = HomePageList(name = request.name, list = items, isHorizontalImages = true),
            hasNext = false
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val channels = getChannels()
        val q = query.lowercase().trim()

        return channels.filter {
            it.name.lowercase().contains(q) || it.category.lowercase().contains(q)
        }.map { channel ->
            newMovieSearchResponse(
                name = channel.name,
                url  = "$mainUrl/player.php?stream=${channel.id}",
                type = TvType.Live
            ) {
                this.posterUrl = channel.poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val streamId = Regex("stream=(\\d+)").find(url)?.groupValues?.get(1) ?: url
        val channels = getChannels()
        val channel  = channels.find { it.id == streamId }

        val channelName = channel?.name ?: "Channel $streamId"
        val channelPoster = channel?.poster
        val category = channel?.category ?: "Live TV"

        return newMovieLoadResponse(
            name    = channelName,
            url     = url,
            type    = TvType.Live,
            dataUrl = url
        ) {
            this.posterUrl = channelPoster
            this.plot      = "RED FORCE TV • $category"
            this.tags      = listOf(category)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (com.lagradost.cloudstream3.utils.ExtractorLink) -> Unit
    ): Boolean {
        try {
            val playerUrl = if (data.startsWith("http")) data else "$mainUrl/player.php?stream=$data"
            val html = app.get(playerUrl, referer = "$mainUrl/").text

            val primaryMatch = Regex("primarySource\\s*=\\s*['\"]([^'\"]+)['\"]").find(html)
            var streamUrl = primaryMatch?.groupValues?.get(1)

            if (streamUrl.isNullOrBlank()) {
                val backupMatch = Regex("backupSource\\s*=\\s*['\"]([^'\"]+)['\"]").find(html)
                streamUrl = backupMatch?.groupValues?.get(1)
            }

            if (!streamUrl.isNullOrBlank()) {
                callback.invoke(
                    com.lagradost.cloudstream3.utils.newExtractorLink(
                        source = this.name,
                        name   = this.name,
                        url    = streamUrl,
                    ) {
                        this.quality = Qualities.Unknown.value
                    }
                )
                return true
            }
        } catch (_: Throwable) {}

        return false
    }
}
