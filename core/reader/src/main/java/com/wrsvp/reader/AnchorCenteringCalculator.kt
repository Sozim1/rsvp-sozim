package com.wrsvp.reader

object AnchorCenteringCalculator {
    fun offsetX(
        containerWidthPx: Float,
        anchorBoundingBoxLeftPx: Float,
        anchorBoundingBoxWidthPx: Float,
    ): Float {
        val anchorCenterX = anchorBoundingBoxLeftPx + anchorBoundingBoxWidthPx / 2f
        val containerCenterX = containerWidthPx / 2f
        return containerCenterX - anchorCenterX
    }
}
