package com.wrsvp.parser

internal data class EpubManifestItem(
    val id: String,
    val href: String,
    val mediaType: String?,
    val properties: String?,
)

internal data class EpubManifest(
    val title: String?,
    val author: String?,
    val language: String?,
    val itemsById: Map<String, EpubManifestItem>,
    val spineIds: List<String>,
)

internal class EpubManifestParser {
    fun parse(opf: String): EpubManifest {
        val items = Regex("""(?is)<item\s+([^>]*?)/?>""")
            .findAll(opf)
            .mapNotNull { match ->
                val attrs = parseAttributes(match.groupValues[1])
                val id = attrs["id"] ?: return@mapNotNull null
                val href = attrs["href"] ?: return@mapNotNull null
                EpubManifestItem(
                    id = id,
                    href = href,
                    mediaType = attrs["media-type"],
                    properties = attrs["properties"],
                )
            }
            .associateBy { it.id }
        val spineIds = Regex("""(?is)<itemref\s+([^>]*?)/?>""")
            .findAll(opf)
            .mapNotNull { parseAttributes(it.groupValues[1])["idref"] }
            .toList()
        return EpubManifest(
            title = Regex("""(?is)<dc:title[^>]*>(.*?)</dc:title>""").find(opf)?.groupValues?.get(1)?.htmlToText()?.trim(),
            author = Regex("""(?is)<dc:creator[^>]*>(.*?)</dc:creator>""").find(opf)?.groupValues?.get(1)?.htmlToText()?.trim(),
            language = Regex("""(?is)<dc:language[^>]*>(.*?)</dc:language>""").find(opf)?.groupValues?.get(1)?.htmlToText()?.trim(),
            itemsById = items,
            spineIds = spineIds,
        )
    }
}

internal class EpubNavParser {
    fun parse(navHtml: String): Map<String, String> {
        val tocBlock = Regex("""(?is)<nav[^>]*(?:epub:type|type)=["'][^"']*toc[^"']*["'][^>]*>(.*?)</nav>""")
            .find(navHtml)
            ?.groupValues
            ?.get(1)
            ?: navHtml
        return Regex("""(?is)<a\s+([^>]*)>(.*?)</a>""")
            .findAll(tocBlock)
            .mapNotNull { match ->
                val href = parseAttributes(match.groupValues[1])["href"] ?: return@mapNotNull null
                val title = match.groupValues[2].htmlToText().trim()
                if (title.isBlank()) null else href.substringBefore('#') to title
            }
            .toMap()
    }
}

internal class EpubNcxParser {
    fun parse(ncx: String): Map<String, String> {
        return Regex("""(?is)<navPoint[^>]*>(.*?)</navPoint>""")
            .findAll(ncx)
            .mapNotNull { navPoint ->
                val block = navPoint.groupValues[1]
                val title = Regex("""(?is)<text[^>]*>(.*?)</text>""")
                    .find(block)
                    ?.groupValues
                    ?.get(1)
                    ?.htmlToText()
                    ?.trim()
                    .orEmpty()
                val src = Regex("""(?is)<content\s+([^>]*)""")
                    .find(block)
                    ?.groupValues
                    ?.get(1)
                    ?.let { parseAttributes(it)["src"] }
                    ?: return@mapNotNull null
                if (title.isBlank()) null else src.substringBefore('#') to title
            }
            .toMap()
    }
}

internal object HtmlToTextConverter {
    fun convert(html: String): String = html.htmlToText().normalizeWhitespace()
}

internal fun parseAttributes(raw: String): Map<String, String> {
    return Regex("""([A-Za-z_:][-A-Za-z0-9_:.]*)\s*=\s*["']([^"']*)["']""")
        .findAll(raw)
        .associate { it.groupValues[1].substringAfter(':').lowercase() to it.groupValues[2] }
}

internal fun normalizeEpubPath(basePath: String, href: String): String {
    val raw = if (basePath.isBlank() || href.startsWith("/")) href.trimStart('/') else "$basePath/$href"
    val parts = ArrayDeque<String>()
    raw.split('/').forEach { part ->
        when {
            part.isBlank() || part == "." -> Unit
            part == ".." -> if (parts.isNotEmpty()) parts.removeLast()
            else -> parts.addLast(part)
        }
    }
    return parts.joinToString("/")
}
