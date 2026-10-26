package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.Qualities

class SkyViewProvider : MainAPI() {
    override var mainUrl = "http://30.30.30.30"
    override var name = "skyView"
    override val supportedTypes = setOf(TvType.Live)
    override var lang = "bn"
    override val hasMainPage = true
    override val hasDownloadSupport = false

    data class SkyViewChannel(
        val id: String,
        val name: String,
        val category: String,
        val poster: String? = null
    )

    companion object {
        private val NAME_MAP = mapOf(
            "101" to "Live Sports",
            "102" to "T Sports",
            "106" to "GTV",
            "110" to "Star Sports 1",
            "115" to "Star Sports 2",
            "119" to "Sony Sports Ten 1",
            "120" to "Sony Sports Ten 2",
            "121" to "Sony Sports Ten 3",
            "123" to "Star Sports Select 1",
            "124" to "Star Sports Select 2",
            "127" to "A Sports",
            "128" to "PTV Sports",
            "201" to "Independent TV",
            "202" to "ATN Bangla",
            "203" to "ATN News",
            "204" to "DBC News",
            "205" to "Somoy TV",
            "206" to "Maasranga TV",
            "207" to "Channel i",
            "209" to "Jamuna TV",
            "301" to "Zee Bangla",
            "302" to "Star Jalsha",
            "303" to "Jalsha Movies",
            "304" to "Colors Bangla",
            "306" to "Sony Aath",
            "307" to "Enterr10 Bangla",
            "401" to "Star Plus",
            "402" to "Star Gold",
            "403" to "Sony Entertainment Television",
            "405" to "Colors Hindi",
            "406" to "Zee Cinema",
            "407" to "Sony Max HD",
            "501" to "Star Movies",
            "503" to "Sony Pix",
            "601" to "Nickelodeon",
            "602" to "Disney Junior",
            "702" to "Sangeet Bangla",
            "703" to "9X Jalwa",
            "801" to "Discovery Channel",
            "802" to "National Geographic"
        )

        private val DEFAULT_CHANNELS = listOf(
            // Sports
            SkyViewChannel("101", "Live Sports", "Sports", "http://30.30.30.30/assets/images/live.png"),
            SkyViewChannel("102", "T Sports", "Sports", "http://30.30.30.30/assets/images/Tsports.png"),
            SkyViewChannel("106", "GTV", "Sports", "http://30.30.30.30/assets/images/GTV.png"),
            SkyViewChannel("110", "Star Sports 1", "Sports", "http://30.30.30.30/assets/images/Star-sports1.png"),
            SkyViewChannel("115", "Star Sports 2", "Sports", "http://30.30.30.30/assets/images/Star-Sports2.png"),
            SkyViewChannel("119", "Sony Sports Ten 1", "Sports", "http://30.30.30.30/assets/images/Sony-Ten1.png"),
            SkyViewChannel("120", "Sony Sports Ten 2", "Sports", "http://30.30.30.30/assets/images/Sony-Ten2.png"),
            SkyViewChannel("121", "Sony Sports Ten 3", "Sports", "http://30.30.30.30/assets/images/Sony-Ten3.png"),
            SkyViewChannel("123", "Star Sports Select 1", "Sports", "http://30.30.30.30/assets/images/Star-Sports-Select1.png"),
            SkyViewChannel("124", "Star Sports Select 2", "Sports", "http://30.30.30.30/assets/images/Star-Sports-Select2.png"),
            SkyViewChannel("127", "A Sports", "Sports", "http://30.30.30.30/assets/images/Asports.png"),
            SkyViewChannel("128", "PTV Sports", "Sports", "http://30.30.30.30/assets/images/PTV.JPG"),

            // Bangla
            SkyViewChannel("301", "Zee Bangla", "Bangla", "http://30.30.30.30/assets/images/Zee-Bangla.png"),
            SkyViewChannel("302", "Star Jalsha", "Bangla", "http://30.30.30.30/assets/images/Star-Jalsha.png"),
            SkyViewChannel("303", "Jalsha Movies", "Bangla", "http://30.30.30.30/assets/images/Jalsha-Movies.png"),
            SkyViewChannel("304", "Colors Bangla", "Bangla", "http://30.30.30.30/assets/images/Colors-Bangla.png"),
            SkyViewChannel("306", "Sony Aath", "Bangla", "http://30.30.30.30/assets/images/Sony-Aath.png"),
            SkyViewChannel("307", "Enterr10 Bangla", "Bangla", "http://30.30.30.30/assets/images/ENTERR10-BANGLA.jpg"),
            SkyViewChannel("201", "Independent TV", "Bangla", "http://30.30.30.30/assets/images/independenttv.png"),
            SkyViewChannel("202", "ATN Bangla", "Bangla", "http://30.30.30.30/assets/images/atnbangla.png"),
            SkyViewChannel("203", "ATN News", "Bangla", "http://30.30.30.30/assets/images/atnnews.png"),
            SkyViewChannel("204", "DBC News", "Bangla", "http://30.30.30.30/assets/images/Dbc.png"),
            SkyViewChannel("206", "Maasranga TV", "Bangla", "http://30.30.30.30/assets/images/Masranga.png"),
            SkyViewChannel("207", "Channel i", "Bangla", "http://30.30.30.30/assets/images/ChannelI.png"),
            SkyViewChannel("209", "Jamuna TV", "Bangla", "http://30.30.30.30/assets/images/Jamunatv.png"),
            SkyViewChannel("205", "Somoy TV", "Bangla", "http://30.30.30.30/assets/images/SomoyTV1689411430.png"),

            // Hindi
            SkyViewChannel("401", "Star Plus", "Hindi", "http://30.30.30.30/assets/images/Star-plus.png"),
            SkyViewChannel("402", "Star Gold", "Hindi", "http://30.30.30.30/assets/images/Star-Gold.png"),
            SkyViewChannel("403", "Sony Entertainment Television", "Hindi", "http://30.30.30.30/assets/images/Sony Entertainment.png"),
            SkyViewChannel("405", "Colors Hindi", "Hindi", "http://30.30.30.30/assets/images/Colors-Hindi.png"),
            SkyViewChannel("406", "Zee Cinema", "Hindi", "http://30.30.30.30/assets/images/Zee-Cinema.png"),
            SkyViewChannel("407", "Sony Max HD", "Hindi", "http://30.30.30.30/assets/images/Sony Max HD1667728133.png"),

            // English & Infotainment
            SkyViewChannel("501", "Star Movies", "English", "http://30.30.30.30/assets/images/Star-Movie.png"),
            SkyViewChannel("503", "Sony Pix", "English", "http://30.30.30.30/assets/images/sony-Pix.png"),
            SkyViewChannel("801", "Discovery Channel", "English", "http://30.30.30.30/assets/images/Discovery.png"),
            SkyViewChannel("802", "National Geographic", "English", "http://30.30.30.30/assets/images/National Geography.png"),

            // Kids & Music (Others)
            SkyViewChannel("601", "Nickelodeon", "others", "http://30.30.30.30/assets/images/NICK.JPG"),
            SkyViewChannel("602", "Disney Junior", "others", "http://30.30.30.30/assets/images/disneyJR.png"),
            SkyViewChannel("703", "9X Jalwa", "others", "http://30.30.30.30/assets/images/9X_Jalwa.png"),
            SkyViewChannel("702", "Sangeet Bangla", "others", "http://30.30.30.30/assets/images/sangeetbangla.png")
        )
    }

    override val mainPage = mainPageOf(
        "all" to "All Channels",
        "Sports" to "Sports",
        "Bangla" to "Bangla",
        "Hindi" to "Hindi",
        "English" to "English & Infotainment",
        "others" to "Kids & Music",
    )

    private var cachedChannels: List<SkyViewChannel>? = null

    private suspend fun getChannels(): List<SkyViewChannel> {
        cachedChannels?.let { return it }

        try {
            val response = app.get("$mainUrl/", timeout = 10).text
            val channels = parseChannels(response)
            if (channels.isNotEmpty()) {
                cachedChannels = channels
                return channels
            }
        } catch (_: Throwable) {
        }

        cachedChannels = DEFAULT_CHANNELS
        return DEFAULT_CHANNELS
    }

    private fun parseChannels(html: String): List<SkyViewChannel> {
        val channels = mutableListOf<SkyViewChannel>()
        val regex = Regex("""<li\s+class=["']([^"']+)["'][^>]*>[\s\S]*?stream=(\d+)[\s\S]*?src=["']([^"']+)["']""")

        regex.findAll(html).forEach { match ->
            val rawCategory = match.groupValues[1].trim()
            val category = if (rawCategory.contains("nav-item", ignoreCase = true)) "Sports" else rawCategory
            val streamId = match.groupValues[2].trim()
            val imgSrc = match.groupValues[3].trim()

            val poster = if (imgSrc.startsWith("http")) imgSrc else "$mainUrl/$imgSrc"
            val name = NAME_MAP[streamId] ?: "Channel $streamId"

            channels.add(SkyViewChannel(id = streamId, name = name, category = category, poster = poster))
        }

        return channels
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val channels = getChannels()
        val filtered = if (request.data == "all") {
            channels
        } else {
            channels.filter { it.category.equals(request.data, ignoreCase = true) }
        }

        val homePageList = filtered.map { channel ->
            newMovieSearchResponse(
                name = channel.name,
                url = "$mainUrl/img/play.php?stream=${channel.id}",
                type = TvType.Live
            ) {
                this.posterUrl = channel.poster
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
        val lower = query.lowercase().trim()

        return channels.filter {
            it.name.lowercase().contains(lower) || it.category.lowercase().contains(lower)
        }.map { channel ->
            newMovieSearchResponse(
                name = channel.name,
                url = "$mainUrl/img/play.php?stream=${channel.id}",
                type = TvType.Live
            ) {
                this.posterUrl = channel.poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val streamId = Regex("""stream=(\d+)""").find(url)?.groupValues?.get(1) ?: url
        val channels = getChannels()
        val channel = channels.find { it.id == streamId }
        val channelName = channel?.name ?: "Channel $streamId"

        return newMovieLoadResponse(
            name = channelName,
            url = url,
            type = TvType.Live,
            dataUrl = url
        ) {
            this.posterUrl = channel?.poster
            this.plot = "SkyView • ${channel?.category ?: "Live TV"}"
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (com.lagradost.cloudstream3.utils.ExtractorLink) -> Unit
    ): Boolean {
        val streamId = Regex("""stream=(\d+)""").find(data)?.groupValues?.get(1) ?: data

        var streamUrl: String? = null

        try {
            val playPageUrl = if (data.startsWith("http")) data else "$mainUrl/img/play.php?stream=$streamId"
            val response = app.get(playPageUrl, timeout = 10).text
            val iframeSrc = Regex("""src=["']([^"']+)["']""").find(response)?.groupValues?.get(1)
            if (!iframeSrc.isNullOrEmpty()) {
                val fullUrl = if (iframeSrc.startsWith("http")) iframeSrc else "$mainUrl/$iframeSrc"
                streamUrl = fullUrl.replace("/embed.html", "/index.m3u8").replace(":8088//", ":8088/")
            }
        } catch (_: Throwable) {
        }

        if (streamUrl == null) {
            streamUrl = "http://30.30.30.30:8088/$streamId/index.m3u8"
        }

        val channel = getChannels().find { it.id == streamId }
        val displayName = channel?.name ?: this.name

        callback.invoke(
            com.lagradost.cloudstream3.utils.newExtractorLink(
                source = this.name,
                name = displayName,
                url = streamUrl,
            ) {
                this.quality = Qualities.P720.value
            }
        )

        return true
    }
}
