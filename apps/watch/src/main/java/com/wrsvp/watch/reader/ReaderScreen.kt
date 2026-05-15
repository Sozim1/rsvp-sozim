package com.wrsvp.watch.reader

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import com.wrsvp.domain.model.FontScale
import com.wrsvp.domain.model.ReaderTheme
import com.wrsvp.domain.model.ReadingMode
import com.wrsvp.reader.AnchorCenteringCalculator
import com.wrsvp.watch.ui.WristReaderColors
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ReaderScreen(
    onBackToLibrary: () -> Unit,
    onOpenChapters: (String) -> Unit = {},
    onOpenDetails: (String) -> Unit = {},
    onReceiveFromComputer: () -> Unit = {},
    onOpenSpeed: (String) -> Unit = {},
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var dragStartX by remember { mutableFloatStateOf(0f) }
    var showQuickMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val colors = readerColors(uiState.theme)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .focusRequester(focusRequester)
            .focusable()
            .onRotaryScrollEvent { event ->
                if (uiState.isPlaying) {
                    if (event.verticalScrollPixels > 0f) viewModel.increaseWpm() else viewModel.decreaseWpm()
                } else {
                    val fastStep = if (uiState.readingMode == ReadingMode.PageScroll) 24 else 12
                    if (event.verticalScrollPixels > 0f) viewModel.skipWords(fastStep) else viewModel.skipWords(-fastStep)
                }
                true
            }
            .pointerInput(uiState.isPlaying) {
                detectTapGestures(
                    onLongPress = { showQuickMenu = true },
                    onTap = {
                        if (uiState.isPlaying) viewModel.pause()
                    },
                )
            }
            .pointerInput(viewModel) {
                detectDragGestures(
                    onDragStart = { offset ->
                        dragX = 0f
                        dragY = 0f
                        dragStartX = offset.x
                    },
                    onDrag = { change, dragAmount ->
                        dragX += dragAmount.x
                        dragY += dragAmount.y
                        change.consume()
                    },
                    onDragEnd = {
                        val leftEdgeLimit = size.width * 0.22f
                        val centerTravel = size.width * 0.34f
                        if (dragStartX <= leftEdgeLimit && dragX >= centerTravel && abs(dragY) < size.height * 0.5f) {
                            viewModel.pause()
                            onBackToLibrary()
                            return@detectDragGestures
                        }
                        if (abs(dragX) > abs(dragY)) {
                            if (dragX < -30f) viewModel.nextWord() else if (dragX > 30f) viewModel.previousWord()
                        } else {
                            if (dragY < -30f) viewModel.increaseWpm() else if (dragY > 30f) viewModel.decreaseWpm()
                        }
                    },
                )
            }
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (uiState.isLoading) {
            Text("Carregando...", color = colors.text)
            return@Box
        }

        ProgressCrown(
            progress = uiState.progressPercent,
            trackColor = colors.ringTrack,
            progressColor = progressRingColor(uiState.progressPercent, colors),
            modifier = Modifier.fillMaxSize(),
        )

        if (uiState.readingMode == ReadingMode.PageScroll) {
            PageScrollReaderText(
                text = uiState.pageText,
                textColor = colors.text,
                onPositionSelected = viewModel::jumpWithinPage,
                modifier = Modifier.padding(horizontal = 30.dp, vertical = 28.dp),
            )
            PageScrollOverlayControls(
                uiState = uiState,
                colors = colors,
                viewModel = viewModel,
                onOpenSpeed = { onOpenSpeed(uiState.bookId) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        } else if (uiState.isPlaying) {
            FocusReader(uiState = uiState, colors = colors)
        } else {
            PausedReaderHud(
                uiState = uiState,
                colors = colors,
                viewModel = viewModel,
                onOpenChapters = { onOpenChapters(uiState.bookId) },
            )
        }

        if (showQuickMenu) {
            QuickReaderMenu(
                uiState = uiState,
                colors = colors,
                viewModel = viewModel,
                onDismiss = { showQuickMenu = false },
                onOpenDetails = {
                    showQuickMenu = false
                    onOpenDetails(uiState.bookId)
                },
                onReceiveFromComputer = {
                    showQuickMenu = false
                    onReceiveFromComputer()
                },
                onOpenSpeed = {
                    showQuickMenu = false
                    onOpenSpeed(uiState.bookId)
                },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun PageScrollOverlayControls(
    uiState: ReaderUiState,
    colors: ReaderPalette,
    viewModel: ReaderViewModel,
    onOpenSpeed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(bottom = 18.dp),
    ) {
        RoundControlButton("RS", colors) { viewModel.switchReadingMode() }
        RoundControlButton(if (uiState.isPlaying) "PAU" else "PLAY", colors) { viewModel.togglePlayPause() }
        RoundControlButton("WPM", colors, onClick = onOpenSpeed)
    }
}

@Composable
private fun FocusReader(uiState: ReaderUiState, colors: ReaderPalette) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        PreviousCurrentNextWordOverlay(
            uiState = uiState,
            colors = colors,
            currentFontSize = focusFontSizeSp(uiState.fontScale),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
        )
        Text(
            text = "${uiState.wpm} WPM",
            color = colors.subtleText,
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 38.dp),
        )
    }
}

@Composable
private fun PreviousCurrentNextWordOverlay(
    uiState: ReaderUiState,
    colors: ReaderPalette,
    currentFontSize: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (uiState.contextOverlayEnabled) {
            ContextWord(uiState.previousToken?.text, colors)
        }
        CenteredAnchorWordText(
            before = uiState.anchorParts.before,
            anchor = uiState.anchorParts.anchor,
            after = uiState.anchorParts.after,
            anchorOffset = uiState.anchorParts.anchorIndex,
            colors = colors,
            fontSize = currentFontSize,
        )
        if (uiState.contextOverlayEnabled) {
            ContextWord(uiState.nextToken?.text, colors)
        }
    }
}

@Composable
private fun ContextWord(text: String?, colors: ReaderPalette) {
    Text(
        text = text.orEmpty(),
        color = colors.subtleText.copy(alpha = 0.58f),
        fontSize = 13.sp,
        maxLines = 1,
    )
}

@Composable
private fun PausedReaderHud(
    uiState: ReaderUiState,
    colors: ReaderPalette,
    viewModel: ReaderViewModel,
    onOpenChapters: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(6.dp))
        StatusPill(text = "PAUSADO", colors = colors)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            CenteredAnchorWordText(
                before = uiState.anchorParts.before,
                anchor = uiState.anchorParts.anchor,
                after = uiState.anchorParts.after,
                anchorOffset = uiState.anchorParts.anchorIndex,
                colors = colors,
                fontSize = pausedFontSizeSp(uiState.fontScale),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoundControlButton("<", colors) { viewModel.previousWord() }
                RoundControlButton("PLAY", colors, emphasis = true) { viewModel.play() }
                RoundControlButton(">", colors) { viewModel.nextWord() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundControlButton(if (uiState.readingMode == ReadingMode.Rsvp) "PS" else "RS", colors) {
                    viewModel.switchReadingMode()
                }
                RoundControlButton("CAP", colors, onClick = onOpenChapters)
            }
            Text(
                text = "${uiState.progressLabel} | ${uiState.currentTokenIndex + 1}/${uiState.totalTokens}",
                color = colors.subtleText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("reader_footer"),
            )
        }
    }
}

@Composable
private fun CenteredAnchorWordText(
    before: String,
    anchor: String,
    after: String,
    anchorOffset: Int,
    colors: ReaderPalette,
    fontSize: Int,
    modifier: Modifier = Modifier,
) {
    // RSVP alignment is based on the anchor glyph center, not the whole word center.
    val word = before + anchor + after
    val annotatedWord = remember(before, anchor, after, colors.accent) {
        buildAnnotatedString {
            append(before)
            withStyle(SpanStyle(color = colors.accent, fontWeight = FontWeight.Black)) {
                append(anchor)
            }
            append(after)
        }
    }
    val textMeasurer = rememberTextMeasurer()
    val textStyle = remember(colors.text, fontSize) {
        TextStyle(
            color = colors.text,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Start,
        )
    }
    // Keep the container measurement across word swaps so autoplay never renders
    // a transient left-aligned frame before the anchor offset is applied.
    var containerWidthPx by remember { mutableIntStateOf(0) }
    val safeAnchorOffset = anchorOffset.coerceIn(0, word.lastIndex.coerceAtLeast(0))
    val textLayout = remember(annotatedWord, textStyle, textMeasurer) {
        textMeasurer.measure(
            text = annotatedWord,
            style = textStyle,
            maxLines = 1,
            softWrap = false,
        )
    }
    val anchorBox = if (word.isNotEmpty()) textLayout.getBoundingBox(safeAnchorOffset) else null
    val offsetXPx = if (anchorBox != null && containerWidthPx > 0) {
        AnchorCenteringCalculator.offsetX(
            containerWidthPx = containerWidthPx.toFloat(),
            anchorBoundingBoxLeftPx = anchorBox.left,
            anchorBoundingBoxWidthPx = anchorBox.width,
        )
    } else {
        0f
    }
    val anchorMarkerXPx = if (anchorBox != null) {
        anchorBox.left + anchorBox.width / 2f + offsetXPx
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { containerWidthPx = it.width },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetXPx.roundToInt(), 0) },
            text = annotatedWord,
            color = colors.text,
            fontSize = fontSize.sp,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Start,
        )
        if (SHOW_ANCHOR_DEBUG && anchorBox != null && containerWidthPx > 0) {
            val anchorCenterX = anchorBox.left + anchorBox.width / 2f
            val containerCenterX = containerWidthPx / 2f
            Log.d(
                "WristRSVP",
                "word=$word anchorOffset=$safeAnchorOffset containerWidthPx=$containerWidthPx " +
                    "anchorBox.left=${anchorBox.left} anchorBox.width=${anchorBox.width} " +
                    "anchorCenterX=$anchorCenterX containerCenterX=$containerCenterX offsetX=$offsetXPx",
            )
        }
        if (SHOW_ANCHOR_DEBUG && containerWidthPx > 0) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val centerX = size.width / 2f
                drawLine(
                    color = Color.Cyan,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = colors.accent,
                    start = Offset(anchorMarkerXPx, size.height * 0.25f),
                    end = Offset(anchorMarkerXPx, size.height * 0.75f),
                    strokeWidth = 2.dp.toPx(),
                )
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, colors: ReaderPalette, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(colors.accent, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QuickReaderMenu(
    uiState: ReaderUiState,
    colors: ReaderPalette,
    viewModel: ReaderViewModel,
    onDismiss: () -> Unit,
    onOpenDetails: () -> Unit,
    onReceiveFromComputer: () -> Unit,
    onOpenSpeed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(colors.background.copy(alpha = 0.96f), RoundedCornerShape(22.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Menu rapido", color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        RoundControlButton("WPM", colors, onClick = onOpenSpeed)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundControlButton("A-", colors) { viewModel.decreaseFontScale() }
            RoundControlButton("A+", colors) { viewModel.increaseFontScale() }
            RoundControlButton(if (uiState.theme == ReaderTheme.Light) "DRK" else "LIG", colors) { viewModel.toggleTheme() }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundControlButton(if (uiState.contextOverlayEnabled) "OVR" else "SEM", colors) { viewModel.toggleContextOverlay() }
            RoundControlButton(if (uiState.readingMode == ReadingMode.Rsvp) "PS" else "RS", colors) {
                viewModel.switchReadingMode()
            }
            RoundControlButton("DET", colors, onClick = onOpenDetails)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundControlButton("PC", colors, onClick = onReceiveFromComputer)
            RoundControlButton("OK", colors, onClick = onDismiss)
        }
    }
}

@Composable
private fun RoundControlButton(
    label: String,
    colors: ReaderPalette,
    emphasis: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(if (emphasis) 52.dp else 44.dp),
        shape = CircleShape,
    ) {
        Text(
            label,
            color = if (emphasis) colors.accent else colors.controlText,
            fontSize = if (emphasis) 10.sp else 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ProgressCrown(
    progress: Float,
    trackColor: Color,
    progressColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 5.dp.toPx()
        val inset = strokeWidth / 2f + 3.dp.toPx()
        val size = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
        )
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = (progress.coerceIn(0f, 1f) * 360f),
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(strokeWidth, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun PageScrollReaderText(
    text: String,
    textColor: Color,
    onPositionSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var offset by remember { mutableFloatStateOf(0f) }
    val scrollState = rememberScrollableState { delta ->
        offset += delta
        delta
    }
    Text(
        text = text,
        color = textColor,
        fontSize = 17.sp,
        lineHeight = 21.sp,
        modifier = modifier
            .scrollable(scrollState, orientation = Orientation.Vertical)
            .pointerInput(text) {
                detectTapGestures { offset ->
                    onPositionSelected(offset.y / size.height.toFloat())
                }
            },
    )
}

private fun fontSizeSp(fontScale: FontScale): Int = when (fontScale) {
    FontScale.Small -> 22
    FontScale.Medium -> 30
    FontScale.Large -> 38
    FontScale.ExtraLarge -> 46
}

private fun pausedFontSizeSp(fontScale: FontScale): Int = when (fontScale) {
    FontScale.Small -> 24
    FontScale.Medium -> 32
    FontScale.Large -> 38
    FontScale.ExtraLarge -> 42
}

private fun focusFontSizeSp(fontScale: FontScale): Int = when (fontScale) {
    FontScale.Small -> 23
    FontScale.Medium -> 31
    FontScale.Large -> 39
    FontScale.ExtraLarge -> 47
}

private data class ReaderPalette(
    val background: Color,
    val text: Color,
    val subtleText: Color,
    val accent: Color,
    val ringTrack: Color,
    val controlText: Color,
)

private fun readerColors(theme: ReaderTheme): ReaderPalette {
    return when (theme) {
        ReaderTheme.Light -> ReaderPalette(
            background = WristReaderColors.IceBackground,
            text = WristReaderColors.PrimaryText,
            subtleText = WristReaderColors.SecondaryText,
            accent = WristReaderColors.Accent,
            ringTrack = WristReaderColors.RingTrack,
            controlText = WristReaderColors.PrimaryText,
        )
        ReaderTheme.Night,
        ReaderTheme.Dark -> ReaderPalette(
            background = WristReaderColors.DarkBackground,
            text = WristReaderColors.DarkText,
            subtleText = WristReaderColors.DarkMuted,
            accent = WristReaderColors.Accent,
            ringTrack = WristReaderColors.DarkRingTrack,
            controlText = WristReaderColors.PrimaryText,
        )
    }
}

private fun progressRingColor(progress: Float, colors: ReaderPalette): Color {
    val remaining = 1f - progress.coerceIn(0f, 1f)
    return lerp(Color(0xFFFFA24D), colors.accent, remaining)
}

private const val SHOW_ANCHOR_DEBUG = false
@Suppress("unused")
private val ANCHOR_DEBUG_WORDS = listOf(
    "eu",
    "casa",
    "relogio",
    "desenvolvimento",
    "internacionalizacao",
    "extraordinariamente",
    "acao",
    "coracao",
    "pao",
    "pre-processamento",
)
