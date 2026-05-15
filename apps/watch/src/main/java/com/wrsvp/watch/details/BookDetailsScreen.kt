package com.wrsvp.watch.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
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
import androidx.wear.compose.material.Text
import com.wrsvp.designsystem.ErrorScreen
import com.wrsvp.watch.ui.LocalWatchPalette

@Composable
fun BookDetailsScreen(
    onContinue: (String) -> Unit,
    onChapters: (String) -> Unit,
    onDeleted: () -> Unit,
    onBack: () -> Unit,
    viewModel: BookDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val book = uiState.book
    val palette = LocalWatchPalette.current
    if (uiState.errorMessage != null || (!uiState.isLoading && book == null)) {
        ErrorScreen(
            title = "Livro indisponivel",
            message = uiState.errorMessage ?: "Nao foi possivel carregar este livro.",
            primaryActionLabel = "Voltar",
            onPrimaryAction = onBack,
            backgroundColor = palette.background,
            titleColor = palette.primaryText,
            messageColor = palette.secondaryText,
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(horizontal = 22.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (uiState.isLoading || book == null) {
            Text("Carregando...", color = palette.primaryText)
            return@Column
        }
        Text(
            text = book.title,
            color = palette.primaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(book.author ?: "Autor desconhecido", color = palette.secondaryText, fontSize = 11.sp)
        Text(book.originLabel(), color = palette.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("${book.totalWords} palavras", color = palette.secondaryText, fontSize = 11.sp)
        Text("${book.totalChapters} capitulos", color = palette.secondaryText, fontSize = 11.sp)
        Text("Checksum ${book.checksum.take(8)}", color = palette.mutedText, fontSize = 10.sp)
        Text(
            "Progresso ${(uiState.progress?.progressPercent?.times(100)?.toInt() ?: 0)}%",
            color = palette.accent,
            fontSize = 11.sp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            Button(onClick = { onContinue(book.id) }, modifier = Modifier.size(118.dp, 42.dp)) {
                Text("Ler", fontSize = 10.sp)
            }
            Button(onClick = { onChapters(book.id) }, modifier = Modifier.size(118.dp, 42.dp)) {
                Text("Cap.", fontSize = 10.sp)
            }
        }
        Text(
            text = "Apagar do relogio",
            color = palette.accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 2.dp)
                .clickable { viewModel.deleteBook(onDeleted) },
        )
    }
}

private fun com.wrsvp.domain.model.Book.originLabel(): String {
    return when {
        id.startsWith("demo") || originalFileName.contains("demo", ignoreCase = true) -> "Origem: demo"
        originalFileType.startsWith("pc:") -> "Recebido do computador"
        else -> "Origem: local"
    }
}
