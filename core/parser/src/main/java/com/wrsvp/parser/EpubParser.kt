package com.wrsvp.parser

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.ZipInputStream

class EpubParser {
    fun canParse(fileName: String): Boolean = fileName.extension() == "epub"

    fun parse(fileName: String, inputStream: InputStream): ParsedBook {
        if (!canParse(fileName)) throw BookParseException("Arquivo EPUB invalido: $fileName")
        return runCatching {
            val entries = readZipEntries(inputStream)
            val container = entries["META-INF/container.xml"]
                ?: throw BookParseException("EPUB sem container.xml")
            val opfPath = Regex("""full-path=["']([^"']+)["']""").find(container)?.groupValues?.get(1)
                ?: throw BookParseException("EPUB sem OPF")
            val opf = entries[opfPath] ?: throw BookParseException("Arquivo OPF nao encontrado")
            val basePath = opfPath.substringBeforeLast('/', missingDelimiterValue = "")
            val manifest = EpubManifestParser().parse(opf)

            val title = manifest.title ?: fileName.nameWithoutExtension()
            if (manifest.spineIds.isEmpty()) throw BookParseException("EPUB sem spine de leitura")

            val navTitles = readNavTitles(entries, basePath, manifest.itemsById)
            val ncxTitles = if (navTitles.isEmpty()) readNcxTitles(entries, basePath, manifest.itemsById) else emptyMap()
            val titleByHref = navTitles + ncxTitles

            val htmlParts = manifest.spineIds.mapNotNull { id ->
                manifest.itemsById[id]?.let { item ->
                    val path = normalizeEpubPath(basePath, item.href)
                    val content = entries[path] ?: entries[item.href]
                    content?.let { item.href to it }
                }
            }
            if (htmlParts.isEmpty()) throw BookParseException("EPUB sem capitulos XHTML legiveis")

            val sections = htmlParts.mapIndexed { index, (href, html) ->
                val hrefKey = normalizeEpubPath(basePath, href).substringBefore('#')
                val localHrefKey = href.substringBefore('#')
                val fileKey = localHrefKey.substringAfterLast('/')
                titleByHref[hrefKey] ?: titleByHref[fileKey] ?:
                titleByHref[localHrefKey] ?:
                Regex("""(?is)<h[1-2][^>]*>(.*?)</h[1-2]>""").find(html)?.groupValues?.get(1)?.htmlToText()?.trim()
                    ?.ifBlank { null }
                    ?: "Capitulo ${index + 1}"
            }.zip(htmlParts.map { HtmlToTextConverter.convert(it.second) })
            val plainText = sections.joinToString("\n\n") { (chapterTitle, text) ->
                if (text.startsWith(chapterTitle)) text else "$chapterTitle\n$text"
            }
            var searchFrom = 0
            val chapters = sections.map { (chapterTitle, _) ->
                val offset = plainText.indexOf(chapterTitle, searchFrom).coerceAtLeast(searchFrom)
                searchFrom = offset + chapterTitle.length
                ParsedChapter(chapterTitle, offset)
            }

            ParsedBook(
                title = title.ifBlank { fileName.nameWithoutExtension() },
                author = manifest.author,
                language = manifest.language,
                chapters = chapters.ifEmpty { listOf(ParsedChapter("Inicio", 0)) },
                plainText = plainText,
                originalFileType = "epub",
            )
        }.getOrElse { throwable ->
            if (throwable is BookParseException) throw throwable
            throw BookParseException("Nao foi possivel ler o EPUB", throwable)
        }
    }

    private fun readZipEntries(inputStream: InputStream): Map<String, String> {
        val entries = linkedMapOf<String, String>()
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.size <= MAX_ENTRY_SIZE_BYTES) {
                    val bytes = zip.readBytes()
                    entries[entry.name] = decodeXmlOrHtml(bytes)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return entries
    }

    private fun readNavTitles(
        entries: Map<String, String>,
        basePath: String,
        manifest: Map<String, EpubManifestItem>,
    ): Map<String, String> {
        val navHref = manifest.values.firstOrNull { item ->
            item.properties?.contains("nav") == true ||
                item.href.endsWith("nav.xhtml", ignoreCase = true) ||
                item.href.endsWith("nav.html", ignoreCase = true)
        } ?: return emptyMap()
        val navPath = normalizeEpubPath(basePath, navHref.href)
        val nav = entries[navPath] ?: entries[navHref.href] ?: return emptyMap()
        return EpubNavParser().parse(nav)
            .mapKeys { (href, _) ->
                normalizeEpubPath(navPath.substringBeforeLast('/', missingDelimiterValue = ""), href)
            }
    }

    private fun readNcxTitles(
        entries: Map<String, String>,
        basePath: String,
        manifest: Map<String, EpubManifestItem>,
    ): Map<String, String> {
        val ncxHref = manifest.values.firstOrNull { item ->
            item.mediaType == "application/x-dtbncx+xml" || item.href.endsWith(".ncx", ignoreCase = true)
        }
            ?: return emptyMap()
        val ncxPath = normalizeEpubPath(basePath, ncxHref.href)
        val ncx = entries[ncxPath] ?: entries[ncxHref.href] ?: return emptyMap()
        return EpubNcxParser().parse(ncx)
            .mapKeys { (href, _) ->
                normalizeEpubPath(ncxPath.substringBeforeLast('/', missingDelimiterValue = ""), href)
            }
    }

    private fun decodeXmlOrHtml(bytes: ByteArray): String {
        val prefix = bytes.take(256).toByteArray().toString(Charsets.ISO_8859_1)
        val charsetName = Regex("""(?i)encoding=["']([^"']+)["']""").find(prefix)?.groupValues?.get(1)
            ?: Regex("""(?i)charset=([A-Za-z0-9._-]+)""").find(prefix)?.groupValues?.get(1)
            ?: "UTF-8"
        val charset = runCatching { Charset.forName(charsetName) }.getOrDefault(Charsets.UTF_8)
        return bytes.toString(charset)
    }

    companion object {
        const val MAX_ENTRY_SIZE_BYTES = 5 * 1024 * 1024

        fun parseBytes(fileName: String, bytes: ByteArray): ParsedBook {
            return EpubParser().parse(fileName, ByteArrayInputStream(bytes))
        }
    }
}
