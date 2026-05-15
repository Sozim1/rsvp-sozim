package com.wrsvp.reader

import com.wrsvp.domain.model.PunctuationType
import com.wrsvp.domain.model.ReaderConfig
import com.wrsvp.domain.model.ReaderSettings
import com.wrsvp.domain.model.ReadingToken

class TextTokenizer(
    private val pacingEngine: ReaderPacingEngine = ReaderPacingEngine(),
    private val config: ReaderConfig = ReaderConfig(),
) {
    private val tokenPattern = Regex("""[\p{L}\p{N}]+(?:[-'][\p{L}\p{N}]+)*[.,;:!?]?""")

    fun tokenize(
        rawText: String,
        chapterIndex: Int = 0,
        settings: ReaderSettings = ReaderSettings(),
    ): List<ReadingToken> {
        val normalized = normalizeInput(rawText)
        var paragraphIndex = 0
        var sentenceIndex = 0
        var wordIndex = 0
        var id = 0L

        return normalized.splitToSequence(Regex("""\n{2,}"""))
            .flatMap { paragraph ->
                val tokens = tokenPattern.findAll(paragraph).map { match ->
                    val text = match.value
                    val punctuation = punctuationType(text)
                    val isSentenceEnd = punctuation == PunctuationType.SentenceEnd
                    val complexity = calculateComplexity(text)
                    val baseToken = ReadingToken(
                        id = id++,
                        text = text,
                        normalizedText = text.trimPunctuation().lowercase(),
                        chapterIndex = chapterIndex,
                        paragraphIndex = paragraphIndex,
                        sentenceIndex = sentenceIndex,
                        wordIndex = wordIndex++,
                        startCharOffset = match.range.first,
                        endCharOffset = match.range.last + 1,
                        punctuationType = punctuation,
                        isSentenceEnd = isSentenceEnd,
                        complexityScore = complexity,
                        estimatedDurationMs = 0L,
                    )
                    val token = baseToken.copy(
                        estimatedDurationMs = pacingEngine.durationMs(baseToken, settings, config),
                    )
                    if (isSentenceEnd) {
                        sentenceIndex += 1
                    }
                    token
                }.toList()
                paragraphIndex += 1
                tokens.asSequence()
            }
            .toList()
    }

    fun normalizeInput(rawText: String): String {
        return rawText
            .replace(Regex("""(?is)<script.*?>.*?</script>"""), " ")
            .replace(Regex("""(?is)<style.*?>.*?</style>"""), " ")
            .replace(Regex("""(?is)<br\s*/?>"""), "\n")
            .replace(Regex("""(?is)</p>|</h[1-6]>|</li>"""), "\n\n")
            .replace(Regex("""(?is)<[^>]+>"""), " ")
            .replace(Regex("""!\[[^]]*]\([^)]*\)"""), " ")
            .replace(Regex("""\[([^]]+)]\([^)]*\)"""), "$1")
            .replace(Regex("""[`*_>#~]+"""), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .lines()
            .joinToString("\n") { it.trim() }
            .replace(Regex("""[ \t]{2,}"""), " ")
            .trim()
    }

    private fun punctuationType(text: String): PunctuationType {
        return when (text.lastOrNull()) {
            '.', '!', '?' -> PunctuationType.SentenceEnd
            ',' -> PunctuationType.Comma
            ':', ';' -> PunctuationType.Colon
            else -> PunctuationType.None
        }
    }

    private fun calculateComplexity(text: String): Int {
        val word = text.trimPunctuation()
        val accents = word.count { it.code > 127 }
        val separators = word.count { it == '-' || it == '\'' }
        val lengthPenalty = if (word.length > config.longWordThreshold) 1 else 0
        return accents + separators + lengthPenalty
    }
}

private fun String.trimPunctuation(): String = trim { it in ".,;:!?" }
