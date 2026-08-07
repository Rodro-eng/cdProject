package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.Qualities

class LalaProvider : MainAPI() {
    override var mainUrl = "http://103.151.60.188:12345"
    override var name = "lala"
    override val supportedTypes = setOf(TvType.Live)
    override var lang = "bn"
    override val hasMainPage = true
    override val hasDownloadSupport = false

    data class LalaChannel(
        val id: String,
        val name: String,
        val url: String
    )

    override val mainPage = mainPageOf(
        "all" to "All Channels",
    )

    private var cachedChannels: List<LalaChannel>? = null

    private suspend fun getChannels(): List<LalaChannel> {
        cachedChannels?.let { return it }

        val response = app.get("$mainUrl/api/channels").text
        val channels = parseChannels(response)
        cachedChannels = channels
        return channels
    }

    private fun parseChannels(json: String): List<LalaChannel> {
        val channels = mutableListOf<LalaChannel>()

        // Parse JSON array manually: [{"id":"...","name":"...","url":"..."},...]
        val itemRegex = Regex("""\{[^}]*"id"\s*:\s*"([^"]*)"[^}]*"name"\s*:\s*"([^"]*)"[^}]*"url"\s*:\s*"([^"]*)"[^}]*\}""")
        itemRegex.findAll(json).forEach { match ->
            val id = match.groupValues[1]
            val name = match.groupValues[2]
                .replace("&amp;", "&")
                .replace("\\u0026", "&")
            val url = match.groupValues[3]

            channels.add(LalaChannel(id = id, name = name, url = url))
        }

        return channels
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val channels = getChannels()

        val homePageList = channels.map { channel ->
            newMovieSearchResponse(
                name = channel.name,
                url = "$mainUrl/api/channels|||${channel.id}|||${channel.url}",
                type = TvType.Live
            )
        }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = homePageList,
                isHorizontalImages = true
            ),
            hasNext = false
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val channels = getChannels()
        val lowerQuery = query.lowercase()

        return channels.filter {
            it.name.lowercase().contains(lowerQuery)
        }.map { channel ->
            newMovieSearchResponse(
                name = channel.name,
                url = "$mainUrl/api/channels|||${channel.id}|||${channel.url}",
                type = TvType.Live
            )
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val parts = url.split("|||")
        val channelId = parts.getOrNull(1) ?: "unknown"
        val streamUrl = parts.getOrNull(2) ?: url

        // Try to find channel name from cache
        val channels = getChannels()
        val channel = channels.find { it.id == channelId }
        val channelName = channel?.name ?: channelId

        return newMovieLoadResponse(
            name = channelName,
            url = url,
            type = TvType.Live,
            dataUrl = streamUrl
        ) {
            this.plot = "La La TV • Live Channel"
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (com.lagradost.cloudstream3.utils.ExtractorLink) -> Unit
    ): Boolean {
        // data is the direct M3U8 stream URL
        callback.invoke(
            com.lagradost.cloudstream3.utils.newExtractorLink(
                source = this.name,
                name = this.name,
                url = data,
            ) {
                this.quality = Qualities.Unknown.value
            }
        )

        return true
    }
}
