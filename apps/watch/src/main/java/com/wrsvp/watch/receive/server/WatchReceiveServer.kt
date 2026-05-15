package com.wrsvp.watch.receive.server

import com.wrsvp.data.BookRepository
import com.wrsvp.parser.BookImportService
import com.wrsvp.parser.BookParseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchReceiveServerRepository @Inject constructor(
    private val bookRepository: BookRepository,
    private val localIpProvider: LocalIpProvider,
    private val pairingCodeGenerator: PairingCodeGenerator,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val parser = BookImportService()
    private var serverSocket: ServerSocket? = null
    private var running = false

    private val _session = MutableStateFlow<WatchReceiveSession?>(null)
    val session: StateFlow<WatchReceiveSession?> = _session.asStateFlow()

    fun start() {
        if (running && _session.value != null) return
        scope.launch {
            stopInternal(markStopped = false)
            val socket = bindSocket()
            val now = System.currentTimeMillis()
            val session = WatchReceiveSession(
                pairingCode = pairingCodeGenerator.generate(),
                localIp = localIpProvider.localIp(),
                port = socket.localPort,
                startedAt = now,
                expiresAt = now + WATCH_RECEIVE_SESSION_TTL_MS,
            )
            serverSocket = socket
            running = true
            _session.value = session
            acceptLoop(socket)
        }
    }

    fun stop() {
        scope.launch { stopInternal(markStopped = true) }
    }

    fun refreshCode() {
        _session.update { session ->
            session?.copy(
                pairingCode = pairingCodeGenerator.generate(),
                expiresAt = System.currentTimeMillis() + WATCH_RECEIVE_SESSION_TTL_MS,
                uploadStatus = WatchUploadStatus.WaitingForUpload,
                lastError = null,
            )
        }
    }

    private suspend fun stopInternal(markStopped: Boolean) {
        running = false
        runCatching { serverSocket?.close() }
        serverSocket = null
        if (markStopped) {
            _session.update { it?.copy(uploadStatus = WatchUploadStatus.Stopped) }
        }
    }

    private fun bindSocket(): ServerSocket {
        for (port in WATCH_RECEIVE_DEFAULT_PORT..WATCH_RECEIVE_DEFAULT_PORT + 3) {
            val socket = runCatching { ServerSocket(port) }.getOrNull()
            if (socket != null) return socket
        }
        error("Portas 8790-8793 indisponiveis")
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (running && !socket.isClosed) {
            val client = runCatching { socket.accept() }.getOrNull() ?: break
            scope.launch { handleClient(client) }
        }
    }

    private suspend fun handleClient(socket: Socket) {
        socket.use { client ->
            val request = readRequest(client) ?: return
            val response = when {
                request.method == "GET" && request.path == "/" -> HttpResponse.html(uploadPage(request.query["code"]))
                request.method == "GET" && request.path == "/health" -> HttpResponse.text("OK")
                request.method == "GET" && request.path == "/status" -> HttpResponse.json(statusJson())
                request.method == "POST" && request.path == "/cancel" -> {
                    stop()
                    HttpResponse.json(JSONObject().put("ok", true))
                }
                request.method == "POST" && request.path == "/upload" -> handleUpload(request)
                else -> HttpResponse.json(JSONObject().put("error", "Endpoint nao encontrado"), 404)
            }
            client.getOutputStream().use { out -> out.write(response.toBytes()) }
        }
    }

    private suspend fun handleUpload(request: HttpRequest): HttpResponse {
        val current = _session.value ?: return errorResponse("Sessao indisponivel", WatchUploadStatus.ServerError, 503)
        if (current.isExpired) return errorResponse("Codigo expirado", WatchUploadStatus.Expired, 403)

        val boundary = request.headers["content-type"]?.substringAfter("boundary=", "")?.trim('"').orEmpty()
        val parts = MultipartRequestParser.parse(request.body, boundary)
        val code = parts.fields["code"].orEmpty()
        if (code != current.pairingCode) return errorResponse("Codigo invalido", WatchUploadStatus.InvalidCode, 403)

        val file = parts.files["file"] ?: return errorResponse("Arquivo nao enviado", WatchUploadStatus.UnsupportedFile, 400)
        if (file.bytes.size > WATCH_RECEIVE_MAX_UPLOAD_BYTES) {
            return errorResponse("Arquivo muito grande", WatchUploadStatus.FileTooLarge, 413)
        }
        val ext = file.fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (ext !in SUPPORTED_TYPES) {
            return errorResponse("Formato nao suportado", WatchUploadStatus.UnsupportedFile, 415)
        }

        _session.update { it?.copy(uploadStatus = WatchUploadStatus.Receiving, lastError = null) }
        return runCatching {
            _session.update { it?.copy(uploadStatus = WatchUploadStatus.Processing) }
            val imported = if (ext == "epub") {
                parser.importEpub(file.bytes.inputStream(), file.fileName)
            } else {
                parser.importText(file.bytes.toString(Charsets.UTF_8), file.fileName, ext)
            }
            val book = imported.book.copy(originalFileType = "pc:$ext")

            _session.update { it?.copy(uploadStatus = WatchUploadStatus.Saving, selectedBookTitle = book.title) }
            bookRepository.saveBookWithTokens(book, imported.chapters, imported.tokens)
            _session.update {
                it?.copy(
                    uploadStatus = WatchUploadStatus.Success,
                    selectedBookTitle = book.title,
                    savedBookId = book.id,
                    lastError = null,
                    expiresAt = System.currentTimeMillis() + 2 * 60 * 1000L,
                )
            }
            HttpResponse.json(
                JSONObject()
                    .put("ok", true)
                    .put("bookId", book.id)
                    .put("title", book.title)
                    .put("totalWords", book.totalWords),
            )
        }.getOrElse { throwable ->
            val message = if (throwable is BookParseException) {
                throwable.message ?: "Nao foi possivel converter o livro"
            } else {
                "Falha ao salvar no relogio"
            }
            errorResponse(message, WatchUploadStatus.SaveFailed, 500)
        }
    }

    private fun errorResponse(message: String, status: WatchUploadStatus, code: Int): HttpResponse {
        _session.update { it?.copy(uploadStatus = status, lastError = message) }
        return HttpResponse.json(JSONObject().put("ok", false).put("error", message), code)
    }

    private fun statusJson(): JSONObject {
        val session = _session.value
        return JSONObject()
            .put("appName", "Sozim RSVP")
            .put("deviceName", "Wear OS")
            .put("sessionId", session?.sessionId)
            .put("expiresAt", session?.expiresAt ?: 0L)
            .put("uploadStatus", session?.uploadStatus?.name ?: WatchUploadStatus.Idle.name)
            .put("currentBookTitle", session?.selectedBookTitle)
            .put("lastError", session?.lastError)
    }

    private fun readRequest(socket: Socket): HttpRequest? {
        val input = BufferedInputStream(socket.getInputStream())
        val headerBytes = ByteArrayOutputStream()
        var matched = 0
        while (true) {
            val b = input.read()
            if (b < 0) return null
            headerBytes.write(b)
            matched = if (b == HEADER_END[matched].toInt()) matched + 1 else if (b == HEADER_END[0].toInt()) 1 else 0
            if (matched == HEADER_END.size) break
            if (headerBytes.size() > 32 * 1024) return null
        }
        val headerText = headerBytes.toByteArray().toString(Charsets.ISO_8859_1)
        val lines = headerText.split("\r\n").filter { it.isNotBlank() }
        val requestLine = lines.firstOrNull()?.split(' ') ?: return null
        val rawTarget = requestLine.getOrNull(1) ?: return null
        val path = rawTarget.substringBefore('?')
        val query = rawTarget.substringAfter('?', "").toQueryMap()
        val headers = lines.drop(1).mapNotNull { line ->
            val key = line.substringBefore(':', "").lowercase(Locale.ROOT)
            val value = line.substringAfter(':', "").trim()
            if (key.isBlank()) null else key to value
        }.toMap()
        val length = headers["content-length"]?.toIntOrNull()?.coerceAtMost(WATCH_RECEIVE_MAX_UPLOAD_BYTES.toInt() + 1024 * 1024) ?: 0
        val body = if (length > 0) input.readNBytes(length) else ByteArray(0)
        return HttpRequest(requestLine[0], path, query, headers, body)
    }

    private companion object {
        val HEADER_END = byteArrayOf('\r'.code.toByte(), '\n'.code.toByte(), '\r'.code.toByte(), '\n'.code.toByte())
        val SUPPORTED_TYPES = setOf("txt", "md", "markdown", "html", "htm", "xhtml", "epub")
    }
}

data class HttpRequest(
    val method: String,
    val path: String,
    val query: Map<String, String>,
    val headers: Map<String, String>,
    val body: ByteArray,
)

data class HttpResponse(
    val code: Int,
    val contentType: String,
    val body: ByteArray,
) {
    fun toBytes(): ByteArray {
        val statusText = when (code) {
            200 -> "OK"
            400 -> "Bad Request"
            403 -> "Forbidden"
            404 -> "Not Found"
            413 -> "Payload Too Large"
            415 -> "Unsupported Media Type"
            500 -> "Internal Server Error"
            503 -> "Service Unavailable"
            else -> "OK"
        }
        val headers = buildString {
            append("HTTP/1.1 $code $statusText\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${body.size}\r\n")
            append("Connection: close\r\n")
            append("Cache-Control: no-store\r\n")
            append("\r\n")
        }.toByteArray(Charsets.UTF_8)
        return headers + body
    }

    companion object {
        fun text(value: String, code: Int = 200): HttpResponse = HttpResponse(code, "text/plain; charset=utf-8", value.toByteArray())
        fun html(value: String, code: Int = 200): HttpResponse = HttpResponse(code, "text/html; charset=utf-8", value.toByteArray())
        fun json(value: JSONObject, code: Int = 200): HttpResponse = HttpResponse(code, "application/json; charset=utf-8", value.toString().toByteArray())
    }
}

data class MultipartFile(
    val fileName: String,
    val bytes: ByteArray,
)

data class MultipartParts(
    val fields: Map<String, String>,
    val files: Map<String, MultipartFile>,
)

object MultipartRequestParser {
    fun parse(body: ByteArray, boundary: String): MultipartParts {
        if (boundary.isBlank()) return MultipartParts(emptyMap(), emptyMap())
        val raw = body.toString(Charsets.ISO_8859_1)
        val marker = "--$boundary"
        val fields = mutableMapOf<String, String>()
        val files = mutableMapOf<String, MultipartFile>()
        raw.split(marker)
            .filter { it.contains("Content-Disposition", ignoreCase = true) }
            .forEach { part ->
                val headerEnd = part.indexOf("\r\n\r\n")
                if (headerEnd < 0) return@forEach
                val headers = part.substring(0, headerEnd)
                val name = Regex("""name="([^"]+)"""").find(headers)?.groupValues?.get(1) ?: return@forEach
                val dataStart = raw.indexOf(part) + headerEnd + 4
                val nextBoundary = raw.indexOf("\r\n$marker", dataStart).takeIf { it >= 0 } ?: raw.length
                val startByte = raw.substring(0, dataStart).toByteArray(Charsets.ISO_8859_1).size
                val endByte = raw.substring(0, nextBoundary).toByteArray(Charsets.ISO_8859_1).size
                val bytes = body.copyOfRange(startByte, endByte)
                val fileName = Regex("""filename="([^"]*)"""").find(headers)?.groupValues?.get(1)
                if (!fileName.isNullOrBlank()) {
                    files[name] = MultipartFile(fileName.substringAfterLast('\\').substringAfterLast('/'), bytes)
                } else {
                    fields[name] = bytes.toString(Charsets.UTF_8).trim()
                }
            }
        return MultipartParts(fields, files)
    }
}

private fun String.toQueryMap(): Map<String, String> {
    if (isBlank()) return emptyMap()
    return split('&').mapNotNull { pair ->
        val parts = pair.split('=', limit = 2)
        if (parts.size == 2) {
            URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts[1], "UTF-8")
        } else {
            null
        }
    }.toMap()
}

private fun uploadPage(prefilledCode: String?): String {
    val codeValue = prefilledCode.orEmpty()
    return """
        <!doctype html>
        <html lang="pt-BR">
        <head>
          <meta charset="utf-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1"/>
          <title>Sozim RSVP</title>
          <style>
            :root { color-scheme: dark; font-family: Inter, Segoe UI, sans-serif; }
            body { margin: 0; min-height: 100vh; display: grid; place-items: center; background: #101827; color: #f8fafc; }
            main { width: min(640px, calc(100vw - 32px)); background: #172033; border: 1px solid #29384f; border-radius: 18px; padding: 28px; box-shadow: 0 24px 70px #0008; }
            h1 { margin: 0; font-size: 30px; }
            p { color: #b8c6dc; }
            label { display: block; margin-top: 16px; color: #9fb0ca; font-size: 13px; font-weight: 700; }
            input { width: 100%; box-sizing: border-box; margin-top: 8px; background: #0f1726; border: 1px solid #33445f; border-radius: 12px; color: #f8fafc; padding: 12px; }
            button { margin-top: 18px; width: 100%; border: 0; border-radius: 12px; background: #ff6b00; color: #111827; padding: 14px; font-weight: 900; cursor: pointer; }
            progress { width: 100%; margin-top: 18px; accent-color: #ff6b00; }
            .status { margin-top: 14px; color: #f8fafc; font-weight: 700; }
          </style>
        </head>
        <body>
          <main>
            <h1>Sozim RSVP</h1>
            <p>Enviar livro para o relogio. Selecione um arquivo para enviar ao seu Galaxy Watch.</p>
            <form id="form">
              <label>Codigo exibido no relogio</label>
              <input name="code" value="$codeValue" inputmode="numeric" maxlength="6" required />
              <label>Livro</label>
              <input name="file" type="file" accept=".txt,.md,.markdown,.html,.htm,.xhtml,.epub" required />
              <button>Enviar para o relogio</button>
            </form>
            <progress id="progress" value="0" max="100"></progress>
            <div class="status" id="status">Aguardando envio...</div>
          </main>
          <script>
            const form = document.getElementById('form');
            const status = document.getElementById('status');
            const progress = document.getElementById('progress');
            form.addEventListener('submit', (event) => {
              event.preventDefault();
              const data = new FormData(form);
              const xhr = new XMLHttpRequest();
              xhr.open('POST', '/upload');
              xhr.upload.onprogress = (e) => {
                if (e.lengthComputable) progress.value = Math.round((e.loaded / e.total) * 100);
                status.textContent = 'Enviando...';
              };
              xhr.onload = () => {
                let body = {};
                try { body = JSON.parse(xhr.responseText || '{}'); } catch (_) {}
                if (xhr.status >= 200 && xhr.status < 300 && body.ok) {
                  progress.value = 100;
                  status.textContent = 'Livro salvo com sucesso: ' + (body.title || '');
                } else {
                  status.textContent = 'Erro: ' + (body.error || 'falha ao salvar no relogio');
                }
              };
              xhr.onerror = () => { status.textContent = 'Erro: falha de conexao com o relogio'; };
              status.textContent = 'Processando no relogio...';
              xhr.send(data);
            });
          </script>
        </body>
        </html>
    """.trimIndent()
}
