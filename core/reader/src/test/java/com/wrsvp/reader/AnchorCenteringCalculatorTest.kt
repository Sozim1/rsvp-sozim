package com.wrsvp.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class AnchorCenteringCalculatorTest {
    @Test
    fun `anchor already centered returns zero offset`() {
        val offset = AnchorCenteringCalculator.offsetX(
            containerWidthPx = 300f,
            anchorBoundingBoxLeftPx = 145f,
            anchorBoundingBoxWidthPx = 10f,
        )

        assertEquals(0f, offset, 0.001f)
    }

    @Test
    fun `anchor left of center returns positive offset`() {
        val offset = AnchorCenteringCalculator.offsetX(
            containerWidthPx = 300f,
            anchorBoundingBoxLeftPx = 90f,
            anchorBoundingBoxWidthPx = 10f,
        )

        assertEquals(55f, offset, 0.001f)
    }

    @Test
    fun `anchor right of center returns negative offset`() {
        val offset = AnchorCenteringCalculator.offsetX(
            containerWidthPx = 300f,
            anchorBoundingBoxLeftPx = 190f,
            anchorBoundingBoxWidthPx = 10f,
        )

        assertEquals(-45f, offset, 0.001f)
    }

    @Test
    fun `long word can produce large offset without clamping`() {
        val offset = AnchorCenteringCalculator.offsetX(
            containerWidthPx = 300f,
            anchorBoundingBoxLeftPx = 24f,
            anchorBoundingBoxWidthPx = 12f,
        )

        assertEquals(120f, offset, 0.001f)
    }

    @Test
    fun `accented word uses measured anchor box independent from text content`() {
        val offset = AnchorCenteringCalculator.offsetX(
            containerWidthPx = 300f,
            anchorBoundingBoxLeftPx = 132f,
            anchorBoundingBoxWidthPx = 14f,
        )

        assertEquals(11f, offset, 0.001f)
    }

    @Test
    fun `offset is not needed again after anchor has been shifted to center`() {
        val firstOffset = AnchorCenteringCalculator.offsetX(
            containerWidthPx = 300f,
            anchorBoundingBoxLeftPx = 90f,
            anchorBoundingBoxWidthPx = 10f,
        )
        val recalcAfterApplyingOffset = AnchorCenteringCalculator.offsetX(
            containerWidthPx = 300f,
            anchorBoundingBoxLeftPx = 90f + firstOffset,
            anchorBoundingBoxWidthPx = 10f,
        )

        assertEquals(0f, recalcAfterApplyingOffset, 0.001f)
    }

    @Test
    fun `font size change recalculates offset from the new measured anchor box`() {
        val smallerFontOffset = AnchorCenteringCalculator.offsetX(
            containerWidthPx = 300f,
            anchorBoundingBoxLeftPx = 112f,
            anchorBoundingBoxWidthPx = 8f,
        )
        val largerFontOffset = AnchorCenteringCalculator.offsetX(
            containerWidthPx = 300f,
            anchorBoundingBoxLeftPx = 96f,
            anchorBoundingBoxWidthPx = 16f,
        )

        assertEquals(34f, smallerFontOffset, 0.001f)
        assertEquals(46f, largerFontOffset, 0.001f)
    }

    @Test
    fun `new word recalculates offset from its own anchor box`() {
        val shortWordOffset = AnchorCenteringCalculator.offsetX(
            containerWidthPx = 300f,
            anchorBoundingBoxLeftPx = 136f,
            anchorBoundingBoxWidthPx = 12f,
        )
        val longWordOffset = AnchorCenteringCalculator.offsetX(
            containerWidthPx = 300f,
            anchorBoundingBoxLeftPx = 72f,
            anchorBoundingBoxWidthPx = 12f,
        )

        assertEquals(8f, shortWordOffset, 0.001f)
        assertEquals(72f, longWordOffset, 0.001f)
    }
}
