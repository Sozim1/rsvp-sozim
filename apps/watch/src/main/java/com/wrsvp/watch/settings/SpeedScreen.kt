package com.wrsvp.watch.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.wear.compose.material.Text
import com.wrsvp.watch.ui.LocalWatchPalette

@Composable
fun SpeedScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val wpm = uiState.settings.wpm.coerceIn(100, 400)
    val palette = LocalWatchPalette.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 22.dp, vertical = 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "VELOCIDADE",
                color = palette.mutedText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                SpeedCircleButton("-", onClick = viewModel::decreaseWpm)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$wpm",
                        color = palette.primaryText,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "WPM",
                        color = palette.mutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                SpeedCircleButton("+", onClick = viewModel::increaseWpm)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    SpeedDot(active = false)
                    SpeedDot(active = true)
                    SpeedDot(active = false)
                }
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(palette.surface)
                        .clickable(onClick = onBack)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Voltar",
                        color = palette.primaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedCircleButton(label: String, onClick: () -> Unit) {
    val palette = LocalWatchPalette.current
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(palette.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = palette.accent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SpeedDot(active: Boolean) {
    val palette = LocalWatchPalette.current
    Box(
        modifier = Modifier
            .size(width = if (active) 14.dp else 5.dp, height = 5.dp)
            .clip(RoundedCornerShape(50))
            .background(if (active) palette.accent else palette.ringTrack),
    )
}
