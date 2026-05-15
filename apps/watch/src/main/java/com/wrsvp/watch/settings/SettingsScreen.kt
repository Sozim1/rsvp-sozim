package com.wrsvp.watch.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.rememberScalingLazyListState
import com.wrsvp.domain.model.FontScale
import com.wrsvp.domain.model.ReaderTheme
import com.wrsvp.watch.ui.LocalWatchPalette

@Composable
fun SettingsScreen(
    onOpenSpeed: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.settings
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
                    text = "Ajustes",
                    color = palette.primaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 18.dp),
                )
            }
            item {
                SettingsCard(title = "Aparencia") {
                    ThemePicker(
                        current = settings.theme,
                        onSelect = viewModel::updateTheme,
                    )
                    ValueRow(
                        label = "Fonte",
                        value = settings.fontScale.label(),
                        onClick = { viewModel.updateFontSize(settings.fontScale.next()) },
                    )
                }
            }
            item {
                SettingsCard(title = "Leitura") {
                    ValueRow(
                        label = "Velocidade",
                        value = "${settings.wpm.coerceIn(100, 400)} WPM",
                        onClick = onOpenSpeed,
                        emphasized = true,
                    )
                }
            }
            item { MutedRow("Envio de livros via PC local, pelo menu da biblioteca.") }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalWatchPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth(0.86f)
            .clip(RoundedCornerShape(24.dp))
            .background(palette.surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title.uppercase(),
            color = palette.mutedText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
        content()
    }
}

@Composable
private fun ValueRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    emphasized: Boolean = false,
) {
    val palette = LocalWatchPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (emphasized) palette.softSurface else palette.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = palette.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (emphasized) palette.surface else palette.softSurface)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value,
                color = if (emphasized) palette.accent else palette.secondaryText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ThemePicker(
    current: ReaderTheme,
    onSelect: (ReaderTheme) -> Unit,
) {
    val palette = LocalWatchPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Tema",
            color = palette.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeChoice(
                label = "Claro",
                selected = current == ReaderTheme.Light,
                onClick = { onSelect(ReaderTheme.Light) },
            )
            ThemeChoice(
                label = "Escuro",
                selected = current != ReaderTheme.Light,
                onClick = { onSelect(ReaderTheme.Dark) },
            )
        }
    }
}

@Composable
private fun ThemeChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalWatchPalette.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) palette.accent else palette.softSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) palette.surface else palette.secondaryText,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MutedRow(text: String) {
    val palette = LocalWatchPalette.current
    Text(
        text = text,
        color = palette.mutedText,
        fontSize = 10.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun FontScale.next(): FontScale = when (this) {
    FontScale.Small -> FontScale.Medium
    FontScale.Medium -> FontScale.Large
    FontScale.Large -> FontScale.ExtraLarge
    FontScale.ExtraLarge -> FontScale.Small
}

private fun FontScale.label(): String = when (this) {
    FontScale.Small -> "Pequena"
    FontScale.Medium -> "Media"
    FontScale.Large -> "Grande"
    FontScale.ExtraLarge -> "Extra"
}
