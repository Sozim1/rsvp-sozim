package com.wrsvp.watch.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wrsvp.watch.receive.server.WatchReceiveServerRepository
import com.wrsvp.watch.receive.server.WatchReceiveSession
import com.wrsvp.watch.receive.server.WatchUploadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ReceiveFromComputerUiState(
    val session: WatchReceiveSession? = null,
    val statusLabel: String = "Iniciando recebimento...",
    val savedBookId: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class ReceiveFromComputerViewModel @Inject constructor(
    private val serverRepository: WatchReceiveServerRepository,
) : ViewModel() {
    val uiState: StateFlow<ReceiveFromComputerUiState> = serverRepository.session
        .map { session ->
            ReceiveFromComputerUiState(
                session = session,
                statusLabel = session?.uploadStatus?.label() ?: "Iniciando recebimento...",
                savedBookId = session?.savedBookId,
                errorMessage = session?.lastError,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReceiveFromComputerUiState())

    init {
        serverRepository.start()
    }

    fun refreshCode() {
        serverRepository.refreshCode()
    }

    fun stopServer() {
        serverRepository.stop()
    }

    override fun onCleared() {
        serverRepository.stop()
        super.onCleared()
    }
}

private fun WatchUploadStatus.label(): String = when (this) {
    WatchUploadStatus.Idle -> "Aguardando..."
    WatchUploadStatus.WaitingForUpload -> "Aguardando envio..."
    WatchUploadStatus.Receiving -> "Recebendo arquivo..."
    WatchUploadStatus.Processing -> "Processando no relogio..."
    WatchUploadStatus.Saving -> "Salvando no relogio..."
    WatchUploadStatus.Success -> "Livro recebido com sucesso"
    WatchUploadStatus.InvalidCode -> "Codigo invalido"
    WatchUploadStatus.UnsupportedFile -> "Formato nao suportado"
    WatchUploadStatus.FileTooLarge -> "Arquivo muito grande"
    WatchUploadStatus.SaveFailed -> "Falha ao salvar"
    WatchUploadStatus.ServerError -> "Erro no servidor local"
    WatchUploadStatus.Expired -> "Codigo expirado"
    WatchUploadStatus.Stopped -> "Recebimento encerrado"
}
