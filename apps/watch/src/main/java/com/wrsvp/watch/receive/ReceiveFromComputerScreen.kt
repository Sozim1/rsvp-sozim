package com.wrsvp.watch.receive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.rememberScalingLazyListState
import com.wrsvp.watch.ui.LocalWatchPalette

@Composable
fun ReceiveFromComputerScreen(
    onOpenBook: (String) -> Unit,
    onBackToLibrary: () -> Unit,
    viewModel: ReceiveFromComputerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val session = uiState.session
    val listState = rememberScalingLazyListState()
    val palette = LocalWatchPalette.current

    DisposableEffect(Unit) {
        onDispose { viewModel.stopServer() }
    }

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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(
                    text = "Receber do computador",
                    color = palette.primaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 18.dp),
                )
            }
            item {
                Text(
                    text = "No computador, acesse:",
                    color = palette.secondaryText,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
            item {
                InfoBlock(
                    title = "URL",
                    value = session?.url ?: "Iniciando...",
                    accent = false,
                )
            }
            item {
                InfoBlock(
                    title = "Codigo",
                    value = session?.pairingCode ?: "------",
                    accent = true,
                )
            }
            item {
                Text(
                    text = uiState.statusLabel,
                    color = palette.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.84f),
                )
            }
            session?.selectedBookTitle?.let { title ->
                item {
                    Text(
                        text = title,
                        color = palette.primaryText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.82f),
                    )
                }
            }
            uiState.errorMessage?.let { error ->
                item {
                    Text(
                        text = error,
                        color = palette.accent,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(0.84f),
                    )
                }
            }
            uiState.savedBookId?.let { bookId ->
                item {
                    Button(onClick = { onOpenBook(bookId) }) {
                        Text("Abrir livro", fontSize = 11.sp)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Button(
                        onClick = viewModel::refreshCode,
                        modifier = Modifier.size(78.dp),
                    ) {
                        Text(
                            text = "Novo\ncodigo",
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.stopServer()
                            onBackToLibrary()
                        },
                        modifier = Modifier.size(78.dp),
                    ) {
                        Text(
                            text = "Encerrar",
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
            item {
                Text(
                    text = "Voltar",
                    color = palette.secondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clickable(onClick = onBackToLibrary),
                )
            }
        }
    }
}

@Composable
private fun InfoBlock(
    title: String,
    value: String,
    accent: Boolean,
) {
    val palette = LocalWatchPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth(0.86f)
            .background(palette.surface, RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = palette.mutedText, fontSize = 9.sp)
        Text(
            text = value,
            color = if (accent) palette.accent else palette.primaryText,
            fontSize = if (accent) 20.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
