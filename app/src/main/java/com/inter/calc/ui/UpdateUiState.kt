package com.inter.calc.ui

import com.inter.calc.update.UpdateInfo

enum class UpdatePhase {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    AVAILABLE,
    DOWNLOADING,
    READY,
    ERROR,
}

data class UpdateUiState(
    val phase: UpdatePhase = UpdatePhase.IDLE,
    val info: UpdateInfo? = null,
    val progress: Float = 0f,
    val error: String? = null,
    val showDialog: Boolean = false,
    val showBanner: Boolean = false,
    val downloadedPath: String? = null,
    val signatureMismatch: Boolean = false,
    val currentVersion: String = "",
    val currentVersionCode: Int = 0,
) {
    val percent: Int get() = (progress * 100f).toInt().coerceIn(0, 100)
}
