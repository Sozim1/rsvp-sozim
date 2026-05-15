package com.wrsvp.parser

class TxtParser : ContentParser {
    override fun canParse(fileName: String): Boolean = fileName.extension() == "txt"

    override fun parse(fileName: String, content: String): ParsedBook {
        val plainText = content.normalizeWhitespace()
        val title = plainText.lineSequence().firstOrNull { it.isNotBlank() }?.trim()
            ?: fileName.nameWithoutExtension()
        return ParsedBook(
            title = title,
            chapters = listOf(ParsedChapter("Inicio", 0)),
            plainText = plainText,
            originalFileType = "txt",
        )
    }
}

class MarkdownParser : ContentParser {
    override fun canParse(fileName: String): Boolean = fileName.extension() in setOf("md", "markdown")

    override fun parse(fileName: String, content: String): ParsedBook {
        val rawTitle = Regex("""(?m)^#\s+(.+)$""").find(content)?.groupValues?.get(1)?.trim()
        val title = rawTitle ?: content.lineSequence().firstOrNull { it.isNotBlank() }
            ?.replace(Regex("""^#+\s*"""), "")
            ?.trim()
            ?: fileName.nameWithoutExtension()

        val plainText = stripMarkdown(content).normalizeWhitespace()
        val chapters = Regex("""(?m)^##\s+(.+)$""")
            .findAll(content)
            .mapIndexed { index, match ->
                val heading = match.groupValues[1].trim()
                ParsedChapter(
                    title = heading.ifBlank { "Capitulo ${index + 1}" },
                    plainTextStartOffset = plainText.indexOf(heading).coerceAtLeast(0),
                )
            }
            .toList()
            .ifEmpty { listOf(ParsedChapter("Inicio", 0)) }

        return ParsedBook(
            title = title,
            chapters = chapters,
            plainText = plainText,
            originalFileType = fileName.extension(),
        )
    }

    private fun stripMarkdown(content: String): String {
        return content
            .replace(Regex("""!\[[^]]*]\([^)]*\)"""), " ")
            .replace(Regex("""\[([^]]+)]\([^)]*\)"""), "$1")
            .replace(Regex("""(?m)^#{1,6}\s*"""), "")
            .replace(Regex("""\*\*([^*]+)\*\*"""), "$1")
            .replace(Regex("""\*([^*]+)\*"""), "$1")
            .replace(Regex("""(?m)^\s*[-*]\s+"""), "")
            .replace(Regex("""(?m)^\s*\d+\.\s+"""), "")
            .replace(Regex("""[`_>~]"""), " ")
    }
}

class HtmlParser : ContentParser {
    override fun canParse(fileName: String): Boolean = fileName.extension() in setOf("html", "htm", "xhtml")

    override fun parse(fileName: String, content: String): ParsedBook {
        val title = Regex("""(?is)<title[^>]*>(.*?)</title>""").find(content)?.groupValues?.get(1)?.htmlToText()?.trim()
            ?: Regex("""(?is)<h1[^>]*>(.*?)</h1>""").find(content)?.groupValues?.get(1)?.htmlToText()?.trim()
            ?: fileName.nameWithoutExtension()

        val chapterMatches = Regex("""(?is)<h2[^>]*>(.*?)</h2>""").findAll(content).toList()
        val plainText = content.htmlToText().normalizeWhitespace()
        val chapters = chapterMatches.mapIndexed { index, match ->
            val heading = match.groupValues[1].htmlToText().trim()
            ParsedChapter(
                title = heading.ifBlank { "Capitulo ${index + 1}" },
                plainTextStartOffset = plainText.indexOf(heading).coerceAtLeast(0),
            )
        }.ifEmpty { listOf(ParsedChapter("Inicio", 0)) }

        return ParsedBook(
            title = title,
            chapters = chapters,
            plainText = plainText,
            originalFileType = fileName.extension(),
        )
    }
}

internal fun String.extension(): String = substringAfterLast('.', "").lowercase()

internal fun String.nameWithoutExtension(): String = substringAfterLast('/').substringBeforeLast('.').ifBlank { "Untitled" }

internal fun String.normalizeWhitespace(): String {
    return lines()
        .joinToString("\n") { it.trim() }
        .replace(Regex("""[ \t]{2,}"""), " ")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()
}

internal fun String.htmlToText(): String {
    return replace(Regex("""(?is)<script.*?>.*?</script>"""), " ")
        .replace(Regex("""(?is)<style.*?>.*?</style>"""), " ")
        .replace(Regex("""(?is)<br\s*/?>"""), "\n")
        .replace(Regex("""(?is)</p>|</div>|</h[1-6]>|</li>"""), "\n\n")
        .replace(Regex("""(?is)<[^>]+>"""), " ")
        .decodeHtmlEntities()
}

internal fun String.decodeHtmlEntities(): String {
    return replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
}
