package com.wrsvp.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class AnchorCalculatorTest {
    private val calculator = AnchorCalculator()

    @Test
    fun `one letter word anchors at zero`() {
        assertEquals(0, calculator.calculate("a").anchorIndex)
    }

    @Test
    fun `short word anchors near beginning`() {
        val parts = calculator.calculate("casa")

        assertEquals("a", parts.anchor)
        assertEquals(1, parts.anchorIndex)
    }

    @Test
    fun `medium word anchors by rule and ratio`() {
        assertEquals(2, calculator.calculate("leitura").anchorIndex)
    }

    @Test
    fun `long word anchors deeper`() {
        assertEquals(4, calculator.calculate("extraordinario").anchorIndex)
    }

    @Test
    fun `accented word keeps anchor parts`() {
        val parts = calculator.calculate("atenção")

        assertEquals("e", parts.anchor)
        assertEquals("at", parts.before)
    }

    @Test
    fun `punctuation is ignored for anchor`() {
        val parts = calculator.calculate("texto.")

        assertEquals("texto", parts.before + parts.anchor + parts.after)
    }
}
