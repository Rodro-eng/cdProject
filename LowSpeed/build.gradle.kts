// Use an integer for version numbers
version = 1

cloudstream {
    // All of these properties are optional, you can safely remove any of them.

    description = "Live TV channels from M3U playlist - Sports, Entertainment, Movies, Cartoon, Bangladesh TV, News, Documentary & Music"
    authors = listOf("Rodro-eng")

    /**
    * Status int as one of the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta-only
    **/
    status = 1

    // List of video source types. Users can filter for extensions providing these types
    tvTypes = listOf(
        "Live",
    )

    iconUrl = "https://imglink.cc/cdn/kIiut6WBq0.jpg"

    language = "bn"
}
