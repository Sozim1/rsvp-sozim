package com.wrsvp.reader

import com.wrsvp.domain.model.ReaderConfig
import com.wrsvp.domain.model.ReaderSettings
import com.wrsvp.domain.model.ReadingToken

class ReaderPacingEngine {
    fun durationMs(
        token: ReadingToken,
        settings: ReaderSettings = ReaderSettings(),
        config: ReaderConfig = ReaderConfig(),
    ): Long {
        val boundedWpm = settings.wpm.coerceIn(config.minWpm, config.maxWpm)
        return 60_000L / boundedWpm
    }

    fun boundedWpm(wpm: Int, settings: ReaderSettings = ReaderSettings()): Int {
        return wpm.coerceIn(settings.minWpm, settings.maxWpm)
    }
}
