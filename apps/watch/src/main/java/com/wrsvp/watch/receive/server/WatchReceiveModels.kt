package com.wrsvp.watch.receive.server

import java.util.UUID

const val WATCH_RECEIVE_DEFAULT_PORT = 8790
const val WATCH_RECEIVE_SESSION_TTL_MS = 10 * 60 * 1000L
const val WATCH_RECEIVE_MAX_UPLOAD_BYTES = 50L * 1024L * 1024L

enum class WatchUploadStatus {
    Idle,
    WaitingForUpload,
    Receiving,
    Processing,
    Saving,
    Success,
    InvalidCode,
    UnsupportedFile,
    FileTooLarge,
    SaveFailed,
    ServerError,
    Expired,
    Stopped,
}

data class WatchReceiveSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val pairingCode: String,
    val localIp: String,
    val port: Int,
    val startedAt: Long,
    val expiresAt: Long,
    val selectedBookTitle: String? = null,
    val savedBookId: String? = null,
    val uploadStatus: WatchUploadStatus = WatchUploadStatus.WaitingForUpload,
    val lastError: String? = null,
) {
    val url: String
        get() = "http://$localIp:$port"

    val urlWithCode: String
        get() = "$url/?code=$pairingCode"

    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt
}
