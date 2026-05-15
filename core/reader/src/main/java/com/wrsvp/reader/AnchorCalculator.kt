package com.wrsvp.reader

import kotlin.math.floor

data class AnchorParts(
    val before: String,
    val anchor: String,
    val after: String,
    val anchorIndex: Int,
)

class AnchorCalculator {
    fun calculate(word: String, anchorDefaultRatio: Double = 0.42): AnchorParts {
        val cleanWord = word.trim { it in ".,;:!?" }
        if (cleanWord.isEmpty()) {
            return AnchorParts(before = "", anchor = "", after = "", anchorIndex = 0)
        }

        val ruleIndex = when (cleanWord.length) {
            1 -> 0
            in 2..5 -> 1
            in 6..9 -> 2
            in 10..13 -> 3
            else -> 4
        }
        val ratioIndex = floor((cleanWord.length - 1) * anchorDefaultRatio).toInt()
        val index = ((ruleIndex + ratioIndex) / 2).coerceIn(0, cleanWord.lastIndex)

        return AnchorParts(
            before = cleanWord.substring(0, index),
            anchor = cleanWord[index].toString(),
            after = cleanWord.substring(index + 1),
            anchorIndex = index,
        )
    }
}
