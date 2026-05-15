package com.wrsvp.watch.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.items
import androidx.wear.compose.material.rememberScalingLazyListState
import com.wrsvp.watch.ui.LocalWatchPalette

@Composable
fun LibraryScreen(
    onOpenBook: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onReceiveFromComputer: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberScalingLazyListState()
    val palette = LocalWatchPalette.current

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    text = "Biblioteca",
                    color = palette.primaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
                )
            }

            if (uiState.isLoading) {
                item { Text("Carregando...", color = palette.primaryText) }
            } else if (uiState.books.isEmpty()) {
                item { Text("Nenhum livro local", color = palette.primaryText) }
            } else {
                items(uiState.books) { item ->
                    BookCard(
                        title = item.book.title,
                        subtitle = item.book.author,
                        progress = item.progressPercent,
                        onClick = { onOpenBook(item.book.id) },
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(palette.accent)
                        .clickable(onClick = onReceiveFromComputer)
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Receber do PC",
                        color = palette.surface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(50))
                        .background(palette.surface)
                        .clickable(onClick = onOpenSettings)
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Ajustes",
                        color = palette.primaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            uiState.errorMessage?.let { message ->
                item { Text(message, color = palette.accent) }
            }
        }
    }
}

@Composable
private fun BookCard(
    title: String,
    subtitle: String?,
    progress: Float,
    onClick: () -> Unit,
) {
    val palette = LocalWatchPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth(0.84f)
            .height(76.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surface)
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    color = palette.primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = palette.secondaryText,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                ProgressBar(progress)
            }
        }
    }
}

@Composable
private fun ProgressBar(progress: Float) {
    val palette = LocalWatchPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .height(4.dp)
            .clip(RoundedCornerShape(50))
            .background(palette.ringTrack),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(palette.accent),
        )
    }
}
