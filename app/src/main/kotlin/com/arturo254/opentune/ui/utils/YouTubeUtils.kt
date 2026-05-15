/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.ui.utils

private const val HighQualityThumbnailSize = 4096

private val youtubeVideoThumbnailRegex =
    Regex(
        pattern = """^(https?://(?:i\d*\.ytimg\.com|img\.youtube\.com)/(vi(?:_webp)?)/[^/?#]+/)[^/?#]+(?:[?#].*)?$""",
        option = RegexOption.IGNORE_CASE,
    )

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this
    rewriteYouTubeVideoThumbnail()?.let { return it }
    "https://lh3\\.googleusercontent\\.com/.*=w(\\d+)-h(\\d+).*".toRegex()
        .matchEntire(this)?.groupValues?.let { group ->
        val (W, H) = group.drop(1).map { it.toInt() }
        var w = width
        var h = height
        if (w != null && h == null) h = (w * H) / W
        if (w == null && h != null) w = (h * W) / H
        return "${split("=w")[0]}=w$w-h$h-p-l90-rj"
    }
    if (this matches "https://yt3\\.ggpht\\.com/.*=s(\\d+).*".toRegex()) {
        return replace(Regex("=s\\d+"), "=s${width ?: height}")
    }
    return this
}

fun String.highQualityThumbnailUrl(): String = resize(HighQualityThumbnailSize, HighQualityThumbnailSize)

fun String?.highQualityThumbnailUrlOrNull(): String? =
    this
        ?.takeIf { it.isNotBlank() }
        ?.highQualityThumbnailUrl()

private fun String.rewriteYouTubeVideoThumbnail(): String? {
    val match = youtubeVideoThumbnailRegex.matchEntire(this) ?: return null
    val prefix = match.groupValues[1]
    val pathType = match.groupValues[2]
    val extension = if (pathType.equals("vi_webp", ignoreCase = true)) "webp" else "jpg"
    return "${prefix}maxresdefault.$extension"
}
