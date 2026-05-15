package com.wrsvp.parser

data class ParsedChapter(
    val title: String,
    val plainTextStartOffset: Int,
)

data class ParsedBook(
    val title: String,
    val author: String? = null,
    val language: String? = null,
    val chapters: List<ParsedChapter> = emptyList(),
    val plainText: String,
    val originalFileType: String,
)

class BookParseException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

interface ContentParser {
    fun canParse(fileName: String): Boolean
    fun parse(fileName: String, content: String): ParsedBook
}
