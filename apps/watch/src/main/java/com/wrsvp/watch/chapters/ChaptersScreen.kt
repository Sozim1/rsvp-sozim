package com.wrsvp.watch.chapters

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.items
import com.wrsvp.designsystem.ErrorScreen
import com.wrsvp.watch.ui.LocalWatchPalette

@Composable
fun ChaptersScreen(
    onOpenReader: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: ChaptersViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val palette = LocalWatchPalette.current
    if (uiState.errorMessage != null) {
        ErrorScreen(
            title = "Erro nos capitulos",
            message = uiState.errorMessage.orEmpty(),
            primaryActionLabel = "Voltar",
            onPrimaryAction = onBack,
            backgroundColor = palette.background,
            titleColor = palette.primaryText,
            messageColor = palette.secondaryText,
        )
        return
    }
    ScalingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("Capitulos", color = palette.primaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        if (uiState.isLoading) {
            item { Text("Carregando...", color = palette.primaryText) }
        } else {
            items(uiState.chapters) { item ->
                ChapterRow(
                    item = item,
                    onClick = {
                        viewModel.selectChapter(item.chapter) {
                            onOpenReader(uiState.bookId)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ChapterRow(item: ChapterUi, onClick: () -> Unit) {
    val palette = LocalWatchPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth(0.86f)
            .clip(RoundedCornerShape(18.dp))
            .background(if (item.isCurrent) palette.softSurface else palette.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = "${item.chapter.index + 1}. ${item.chapter.title}",
            color = palette.primaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${(item.progressPercent * 100).toInt()}%",
            color = palette.accent,
            fontSize = 10.sp,
        )
    }
}
