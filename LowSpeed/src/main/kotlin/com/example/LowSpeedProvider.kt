package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities

class LowSpeedProvider : MainAPI() {
    override var mainUrl = "https://go.skym3u.top"
    override var name = "LowSpeed"
    override val supportedTypes = setOf(TvType.Live)
    override var lang = "bn"
    override val hasMainPage = true
    override val hasDownloadSupport = false

    companion object {
        private const val M3U_URL = "https://go.skym3u.top/ik5m.m3u"
    }

    // Data class to hold parsed channel info
    data class M3uChannel(
        val name: String,
        val group: String,
        val logo: String,
        val url: String
    )

    // Main page sections based on M3U group-title categories
    override val mainPage = mainPageOf(
        "Sports" to "Sports",
        "Entertainment" to "Entertainment",
        "Movies" to "Movies",
        "Music" to "Music",
        "Cartoon" to "Cartoon",
        "Bangladesh" to "Bangladesh",
        "News" to "News",
        "Documentary" to "Documentary",
    )

    // Cache parsed channels to avoid re-fetching
    private var cachedChannels: List<M3uChannel>? = null

    private suspend fun getChannels(): List<M3uChannel> {
        cachedChannels?.let { return it }

        val response = app.get(M3U_URL).text
        val channels = parseM3u(response)
        cachedChannels = channels
        return channels
    }

    private fun parseM3u(content: String): List<M3uChannel> {
        val channels = mutableListOf<M3uChannel>()
        val lines = content.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF:")) {
                // Parse the EXTINF line
                val groupMatch = Regex("""group-title="([^"]*?)"""").find(line)
                val logoMatch = Regex("""tvg-logo="([^"]*?)"""").find(line)
                // Channel name is after the last comma
                val nameMatch = line.substringAfterLast(",").trim()

                val group = groupMatch?.groupValues?.get(1) ?: "Other"
                val logo = logoMatch?.groupValues?.get(1) ?: ""
                val channelName = nameMatch

                // Next non-empty, non-comment line should be the URL
                var j = i + 1
                while (j < lines.size) {
                    val nextLine = lines[j].trim()
                    if (nextLine.isNotEmpty() && !nextLine.startsWith("#")) {
                        channels.add(
                            M3uChannel(
                                name = channelName,
                                group = group,
                                logo = logo,
                                url = nextLine
                            )
                        )
                        break
                    }
                    j++
                }
            }
            i++
        }

        return channels
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val channels = getChannels()
        val groupName = request.data // This is the key from mainPage map

        val filteredChannels = channels.filter { it.group == groupName }

        val homePageList = filteredChannels.map { channel ->
            newMovieSearchResponse(
                name = channel.name,
                url = channel.url,
                type = TvType.Live
            ) {
                this.posterUrl = channel.logo
            }
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
            it.name.lowercase().contains(lowerQuery) ||
            it.group.lowercase().contains(lowerQuery)
        }.map { channel ->
            newMovieSearchResponse(
                name = channel.name,
                url = channel.url,
                type = TvType.Live
            ) {
                this.posterUrl = channel.logo
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val channels = getChannels()
        val channel = channels.find { it.url == url }

        val channelName = channel?.name ?: "Unknown Channel"
        val channelLogo = channel?.logo ?: ""
        val channelGroup = channel?.group ?: "Other"

        return newMovieLoadResponse(
            name = channelName,
            url = url,
            type = TvType.Live,
            dataUrl = url
        ) {
            this.posterUrl = channelLogo
            this.plot = "Live TV • $channelGroup"
            this.tags = listOf(channelGroup)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val channels = getChannels()
        val channel = channels.find { it.url == data }
        val channelName = channel?.name ?: "LowSpeed"

        callback.invoke(
            ExtractorLink(
                source = this.name,
                name = channelName,
                url = data,
                referer = "",
                quality = Qualities.Unknown.value,
                isM3u8 = data.contains(".m3u8") || data.contains(".ts"),
            )
        )

        return true
    }
}
