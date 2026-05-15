package com.wrsvp.watch.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.wrsvp.domain.model.ReaderTheme

data class WatchPalette(
    val background: Color,
    val surface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val accent: Color,
    val ringTrack: Color,
    val softSurface: Color,
)

fun watchPalette(theme: ReaderTheme): WatchPalette {
    return when (theme) {
        ReaderTheme.Light -> WatchPalette(
            background = WristReaderColors.IceBackground,
            surface = WristReaderColors.Surface,
            primaryText = WristReaderColors.PrimaryText,
            secondaryText = WristReaderColors.SecondaryText,
            mutedText = WristReaderColors.MutedText,
            accent = WristReaderColors.Accent,
            ringTrack = WristReaderColors.RingTrack,
            softSurface = WristReaderColors.IceBackground,
        )
        ReaderTheme.Night,
        ReaderTheme.Dark -> WatchPalette(
            background = WristReaderColors.DarkBackground,
            surface = WristReaderColors.DarkSurface,
            primaryText = WristReaderColors.DarkText,
            secondaryText = WristReaderColors.DarkMuted,
            mutedText = WristReaderColors.DarkMuted.copy(alpha = 0.82f),
            accent = WristReaderColors.Accent,
            ringTrack = WristReaderColors.DarkRingTrack,
            softSurface = WristReaderColors.DarkSurface.copy(alpha = 0.9f),
        )
    }
}

val LocalWatchPalette = compositionLocalOf { watchPalette(ReaderTheme.Light) }
