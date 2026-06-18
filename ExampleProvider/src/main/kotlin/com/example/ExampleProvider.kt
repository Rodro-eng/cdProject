package com.example

import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.utils.ExtractorLink

class ExampleProvider : MainAPI() { // All providers must be an instance of MainAPI
    override var mainUrl = "https://example.com/"
    override var name = "Example provider"
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
    )

    override var lang = "en"

    // Enable this when your provider has a main page
    override val hasMainPage = true

    // This is needed to set the correct referer header for requests
    // override val mainPage = mainPageOf(
    //     "$mainUrl/movies" to "Latest Movies",
    //     "$mainUrl/series" to "Latest Series",
    // )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Use this to get the homepage
        TODO("Return the home page")
    }

    override suspend fun search(query: String): List<SearchResponse> {
        // Use this to search for content
        TODO("Return search results")
    }

    override suspend fun load(url: String): LoadResponse {
        // Use this to load the details of a specific content
        TODO("Return load response")
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // Use this to load the video links
        TODO("Return video links")
    }
}
