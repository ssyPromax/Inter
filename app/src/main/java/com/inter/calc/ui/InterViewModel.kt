package com.inter.calc.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.inter.calc.BuildConfig
import com.inter.calc.data.ApiSettingsStore
import com.inter.calc.data.ChatMessage
import com.inter.calc.data.DeepSeekClient
import com.inter.calc.data.DeepSeekConfig
import com.inter.calc.math.CalculusEngine
import com.inter.calc.math.CurvePoint
import com.inter.calc.math.Expr
import com.inter.calc.math.ExprParser
import com.inter.calc.math.NumericIntegrator
import com.inter.calc.update.ApkVerifier
import com.inter.calc.update.InstallResult
import com.inter.calc.update.UpdateClient
import com.inter.calc.update.UpdateConfig
import com.inter.calc.update.UpdateInfo
import com.inter.calc.update.UpdateInstaller
import com.inter.calc.update.UpdateSettingsStore
import com.inter.calc.update.isNewerVersion
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InterUiState(
    val input: TextFieldValue = TextFieldValue("", TextRange(0)),
    val darkTheme: Boolean = false,
    val showKeypad: Boolean = true,
    val originalExpr: Expr? = null,
    val derivativeExpr: Expr? = null,
    val integralExpr: Expr? = null,
    val originalLatex: String = "",
    val derivativeLatex: String = "",
    val integralLatex: String = "",
    val originalLabel: String = "",
    val derivativeLabel: String = "",
    val integralLabel: String = "",
    val integralIsNumericOnly: Boolean = false,
    val parseError: String? = null,
    val xCenter: Double = 0.0,
    val yCenter: Double = 0.0,
    val xHalfSpan: Double = 6.0,
    val showOriginal: Boolean = true,
    val showDerivative: Boolean = true,
    val showIntegral: Boolean = true,
    val originalPoints: List<CurvePoint> = emptyList(),
    val derivativePoints: List<CurvePoint> = emptyList(),
    val integralPoints: List<CurvePoint> = emptyList(),
    val deepSeekConfig: DeepSeekConfig = DeepSeekConfig(),
    val showApiDialog: Boolean = false,
    val showExplainScreen: Boolean = false,
    val explainMessages: List<ChatMessage> = emptyList(),
    val isStreamingExplain: Boolean = false,
    val explainError: String? = null,
    val update: UpdateUiState = UpdateUiState(),
)

class InterViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ApiSettingsStore(application)
    private val updateSettings = UpdateSettingsStore(application)

    private val _uiState = MutableStateFlow(
        InterUiState(
            darkTheme = store.loadDarkTheme(),
            deepSeekConfig = store.loadConfig(),
            update = UpdateUiState(
                currentVersion = BuildConfig.VERSION_NAME,
                currentVersionCode = BuildConfig.VERSION_CODE,
            ),
        )
    )
    val uiState: StateFlow<InterUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null
    private var updateJob: Job? = null

    init {
        viewModelScope.launch { autoCheckForUpdates() }
    }

    fun toggleDarkTheme() {
        val next = !_uiState.value.darkTheme
        store.saveDarkTheme(next)
        _uiState.update { it.copy(darkTheme = next) }
    }

    fun setKeypadVisible(visible: Boolean) {
        _uiState.update { it.copy(showKeypad = visible) }
    }

    fun onInputChange(value: TextFieldValue) {
        _uiState.update { it.copy(input = value) }
        recomputeAll(value.text)
    }

    fun insertToken(token: String) {
        val current = _uiState.value.input
        val text = current.text
        val start = current.selection.start.coerceIn(0, text.length)
        val end = current.selection.end.coerceIn(0, text.length)
        val newText = text.substring(0, start) + token + text.substring(end)
        onInputChange(TextFieldValue(newText, TextRange(start + token.length)))
    }

    fun insertFraction() {
        val current = _uiState.value.input
        val text = current.text
        val start = current.selection.start.coerceIn(0, text.length)
        val end = current.selection.end.coerceIn(0, text.length)
        val before = text.substring(0, start)
        val after = text.substring(end)

        if (before.isEmpty() || before.last() in listOf('+', '-', '*', '/', '(', '^')) {
            val inserted = "()/()"
            val newText = before + inserted + after
            onInputChange(TextFieldValue(newText, TextRange(start + 1)))
        } else {
            val inserted = "/()"
            val newText = before + inserted + after
            onInputChange(TextFieldValue(newText, TextRange(start + 2)))
        }
    }

    fun moveCursor(delta: Int) {
        val current = _uiState.value.input
        val target = (current.selection.end + delta).coerceIn(0, current.text.length)
        _uiState.update { it.copy(input = current.copy(selection = TextRange(target))) }
    }

    fun backspace() {
        val current = _uiState.value.input
        val text = current.text
        val start = current.selection.start.coerceIn(0, text.length)
        val end = current.selection.end.coerceIn(0, text.length)
        if (start != end) {
            onInputChange(TextFieldValue(text.substring(0, start) + text.substring(end), TextRange(start)))
        } else if (start > 0) {
            onInputChange(TextFieldValue(text.substring(0, start - 1) + text.substring(start), TextRange(start - 1)))
        }
    }

    fun clearInput() { onInputChange(TextFieldValue("", TextRange(0))) }

    fun toggleOriginalCurve() = _uiState.update { it.copy(showOriginal = !it.showOriginal) }
    fun toggleDerivativeCurve() = _uiState.update { it.copy(showDerivative = !it.showDerivative) }
    fun toggleIntegralCurve() = _uiState.update { it.copy(showIntegral = !it.showIntegral) }

    fun transformViewport(dxFrac: Double, dyFrac: Double, zoomFactor: Double) {
        _uiState.update { s ->
            val clampedZoom = zoomFactor.coerceIn(0.6, 1.6)
            val newHalfSpan = (s.xHalfSpan / clampedZoom).coerceIn(0.5, 60.0)
            s.copy(
                xCenter = s.xCenter - dxFrac * (2.0 * newHalfSpan),
                yCenter = s.yCenter + dyFrac * (2.0 * newHalfSpan * 0.65),
                xHalfSpan = newHalfSpan,
            )
        }
        resampleCurves()
    }

    fun resetViewport() {
        _uiState.update { it.copy(xCenter = 0.0, yCenter = 0.0, xHalfSpan = 6.0) }
        resampleCurves()
    }

    private fun recomputeAll(rawText: String) {
        if (rawText.isBlank()) {
            _uiState.update {
                it.copy(
                    originalExpr = null, derivativeExpr = null, integralExpr = null,
                    originalLatex = "", derivativeLatex = "", integralLatex = "",
                    originalLabel = "", derivativeLabel = "", integralLabel = "",
                    integralIsNumericOnly = false,
                    parseError = null,
                    originalPoints = emptyList(), derivativePoints = emptyList(), integralPoints = emptyList(),
                )
            }
            return
        }
        val parsed = runCatching { ExprParser.parse(rawText) }
        if (parsed.isFailure) {
            _uiState.update { it.copy(parseError = parsed.exceptionOrNull()?.message ?: "表达式格式有误") }
            return
        }
        val expr = parsed.getOrThrow()
        val deriv = CalculusEngine.differentiate(expr)
        val integ = CalculusEngine.integrate(expr)
        _uiState.update {
            it.copy(
                originalExpr = expr,
                derivativeExpr = deriv,
                integralExpr = integ,
                originalLatex = expr.toLatex(),
                derivativeLatex = deriv.toLatex(),
                integralLatex = integ?.toLatex() ?: "",
                originalLabel = expr.format(),
                derivativeLabel = deriv.format(),
                integralLabel = integ?.format() ?: "数值积分",
                integralIsNumericOnly = integ == null,
                parseError = null,
            )
        }
        resampleCurves()
    }

    private fun resampleCurves() {
        val s = _uiState.value
        val expr = s.originalExpr ?: return
        val xMin = s.xCenter - s.xHalfSpan
        val xMax = s.xCenter + s.xHalfSpan
        _uiState.update {
            it.copy(
                originalPoints = NumericIntegrator.sampleFunction(expr, xMin, xMax),
                derivativePoints = NumericIntegrator.sampleDerivative(expr, s.derivativeExpr, xMin, xMax),
                integralPoints = NumericIntegrator.sampleIntegral(expr, s.integralExpr, xMin, xMax),
            )
        }
    }

    fun openApiSettings() = _uiState.update { it.copy(showApiDialog = true) }
    fun closeApiSettings() = _uiState.update { it.copy(showApiDialog = false) }

    fun saveApiSettings(config: DeepSeekConfig) {
        store.saveConfig(config)
        val shouldAutoExplain = _uiState.value.showExplainScreen && _uiState.value.explainMessages.isEmpty() && config.isConfigured
        _uiState.update { it.copy(deepSeekConfig = config, showApiDialog = false) }
        if (shouldAutoExplain) startInitialExplanation()
    }

    fun openExplainScreen() {
        val s = _uiState.value
        if (s.originalExpr == null) return
        _uiState.update { it.copy(showExplainScreen = true, explainMessages = emptyList(), explainError = null) }
        if (s.deepSeekConfig.isConfigured) startInitialExplanation()
    }

    fun closeExplainScreen() {
        streamJob?.cancel()
        _uiState.update { it.copy(showExplainScreen = false, isStreamingExplain = false) }
    }

    fun startInitialExplanation() {
        val s = _uiState.value
        val tex = s.originalLatex.ifBlank { s.input.text }
        val prompt = "求不定积分 \$\\int ($tex)\\,dx\$，给出 2～3 种最常用方法。所有公式用 \\begin{aligned} 多行对齐，每行最多一个等号，文字极简。"
        _uiState.update { it.copy(explainMessages = emptyList(), explainError = null) }
        sendDeepSeekTurn(prompt)
    }

    fun sendFollowUpQuestion(question: String) {
        if (question.isBlank() || _uiState.value.isStreamingExplain) return
        sendDeepSeekTurn(question.trim())
    }

    private fun sendDeepSeekTurn(userText: String) {
        streamJob?.cancel()
        val config = _uiState.value.deepSeekConfig
        val updatedHistory = _uiState.value.explainMessages + ChatMessage("user", userText)
        val withPlaceholder = updatedHistory + ChatMessage("assistant", "")
        _uiState.update { it.copy(explainMessages = withPlaceholder, isStreamingExplain = true, explainError = null) }

        streamJob = viewModelScope.launch {
            val sb = StringBuilder()
            try {
                DeepSeekClient.streamChat(config, updatedHistory).collect { chunk ->
                    sb.append(chunk)
                    val content = sb.toString()
                    _uiState.update { state ->
                        val msgs = state.explainMessages.toMutableList()
                        if (msgs.isNotEmpty() && msgs.last().role == "assistant") {
                            msgs[msgs.lastIndex] = ChatMessage("assistant", content)
                        }
                        state.copy(explainMessages = msgs)
                    }
                }
                _uiState.update { it.copy(isStreamingExplain = false) }
            } catch (e: Exception) {
                _uiState.update { state ->
                    val msgs = state.explainMessages.toMutableList()
                    if (msgs.isNotEmpty() && msgs.last().role == "assistant" && msgs.last().content.isEmpty()) {
                        msgs.removeAt(msgs.lastIndex)
                    }
                    state.copy(
                        explainMessages = msgs,
                        isStreamingExplain = false,
                        explainError = e.message ?: "请求 DeepSeek 失败，请检查网络或 API Key",
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------------ 更新

    private fun autoCheckForUpdates() {
        if (System.currentTimeMillis() - updateSettings.lastCheckAt() < UpdateConfig.AUTO_CHECK_INTERVAL_MS) {
            return
        }
        checkForUpdates(showResult = false)
    }

    fun checkForUpdates(showResult: Boolean = true) {
        val current = _uiState.value.update
        if (current.phase == UpdatePhase.CHECKING) {
            if (showResult) {
                _uiState.update { it.copy(update = it.update.copy(showDialog = true)) }
            }
            return
        }
        if (current.phase == UpdatePhase.DOWNLOADING) {
            if (showResult) {
                _uiState.update { it.copy(update = it.update.copy(showDialog = true)) }
            }
            return
        }

        _uiState.update {
            it.copy(
                update = it.update.copy(
                    phase = UpdatePhase.CHECKING,
                    showDialog = showResult || it.update.showDialog,
                    error = null,
                    signatureMismatch = false,
                )
            )
        }
        updateJob = viewModelScope.launch {
            try {
                val info = UpdateClient.check()
                updateSettings.markChecked()
                if (isUpdateNewer(info)) {
                    val autoDownload = !showResult &&
                        updateSettings.lastAutoDownloadVersion() != info.versionName &&
                        isOnWifi()
                    _uiState.update {
                        it.copy(
                            update = it.update.copy(
                                phase = UpdatePhase.AVAILABLE,
                                info = info,
                                progress = 0f,
                                error = null,
                                downloadedPath = null,
                                signatureMismatch = false,
                                showBanner = !updateSettings.isBannerDismissed(info.versionName),
                                showDialog = showResult || it.update.showDialog,
                            )
                        )
                    }
                    if (autoDownload) {
                        updateSettings.markAutoDownloadVersion(info.versionName)
                        startDownload(showDialog = false)
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            update = it.update.copy(
                                phase = UpdatePhase.UP_TO_DATE,
                                info = info,
                                showBanner = false,
                                showDialog = showResult || it.update.showDialog,
                                error = null,
                            )
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (showResult) {
                    _uiState.update {
                        it.copy(
                            update = it.update.copy(
                                phase = UpdatePhase.ERROR,
                                error = e.message ?: "检查更新失败，请稍后再试。",
                                showDialog = true,
                                showBanner = it.update.info != null,
                            )
                        )
                    }
                } else {
                    _uiState.update { it.copy(update = it.update.copy(phase = UpdatePhase.IDLE)) }
                }
            }
        }
    }

    fun downloadUpdate() {
        startDownload(showDialog = true)
    }

    private fun startDownload(showDialog: Boolean) {
        val state = _uiState.value.update
        val info = state.info ?: return
        if (state.phase == UpdatePhase.DOWNLOADING) return

        _uiState.update {
            it.copy(
                update = it.update.copy(
                    phase = UpdatePhase.DOWNLOADING,
                    progress = 0f,
                    error = null,
                    signatureMismatch = false,
                    showDialog = showDialog || it.update.showDialog,
                )
            )
        }
        updateJob = viewModelScope.launch {
            try {
                val file = UpdateClient.download(info, updateDir()) { progress ->
                    _uiState.update { currentState ->
                        currentState.copy(update = currentState.update.copy(progress = progress))
                    }
                }
                val check = ApkVerifier.verify(getApplication(), file, BuildConfig.VERSION_CODE)
                if (!check.ok) {
                    file.delete()
                    _uiState.update {
                        it.copy(
                            update = it.update.copy(
                                phase = UpdatePhase.ERROR,
                                error = check.message ?: "下载的安装包未通过校验。",
                                signatureMismatch = check.signatureMismatch,
                                showBanner = true,
                                showDialog = it.update.showDialog,
                            )
                        )
                    }
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        update = it.update.copy(
                            phase = UpdatePhase.READY,
                            progress = 1f,
                            downloadedPath = file.absolutePath,
                            error = null,
                            showBanner = !updateSettings.isBannerDismissed(info.versionName),
                            showDialog = it.update.showDialog,
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        update = it.update.copy(
                            phase = UpdatePhase.ERROR,
                            error = e.message ?: "下载失败，请重试。",
                            showBanner = true,
                            showDialog = it.update.showDialog,
                        )
                    )
                }
            }
        }
    }

    fun installUpdate() {
        val path = _uiState.value.update.downloadedPath
        if (path.isNullOrBlank()) {
            _uiState.update {
                it.copy(update = it.update.copy(error = "安装包还没有下载完成。", showDialog = true))
            }
            return
        }
        val file = File(path)
        if (!file.isFile) {
            _uiState.update {
                it.copy(
                    update = it.update.copy(
                        phase = UpdatePhase.ERROR,
                        error = "安装包已不存在，请重新下载。",
                        showDialog = true,
                    )
                )
            }
            return
        }
        when (val result = UpdateInstaller.install(getApplication(), file)) {
            InstallResult.Started -> Unit
            InstallResult.NeedPermission -> _uiState.update {
                it.copy(
                    update = it.update.copy(
                        error = "请允许 Inter 安装未知应用，然后返回这里再点一次“安装更新”。",
                        showDialog = true,
                    )
                )
            }
            is InstallResult.Failed -> _uiState.update {
                it.copy(
                    update = it.update.copy(
                        phase = UpdatePhase.ERROR,
                        error = result.message,
                        showDialog = true,
                    )
                )
            }
        }
    }

    fun openUpdateDialog() {
        val state = _uiState.value.update
        if (state.info == null && state.phase != UpdatePhase.ERROR && state.phase != UpdatePhase.UP_TO_DATE) {
            checkForUpdates(showResult = true)
        } else {
            _uiState.update { it.copy(update = it.update.copy(showDialog = true)) }
        }
    }

    fun openUpdateFromSettings() {
        _uiState.update { it.copy(showApiDialog = false) }
        val state = _uiState.value.update
        if (state.phase == UpdatePhase.AVAILABLE || state.phase == UpdatePhase.READY ||
            state.phase == UpdatePhase.DOWNLOADING || state.phase == UpdatePhase.ERROR
        ) {
            _uiState.update { it.copy(update = it.update.copy(showDialog = true)) }
        } else {
            checkForUpdates(showResult = true)
        }
    }

    fun dismissUpdateDialog() {
        _uiState.update { it.copy(update = it.update.copy(showDialog = false)) }
    }

    fun dismissUpdateBanner() {
        val info = _uiState.value.update.info
        if (info != null) {
            updateSettings.dismissBanner(info.versionName)
        }
        _uiState.update { it.copy(update = it.update.copy(showBanner = false)) }
    }

    fun retryUpdate() {
        val state = _uiState.value.update
        if (state.signatureMismatch) return
        if (state.info != null && state.phase == UpdatePhase.ERROR) {
            startDownload(showDialog = true)
        } else {
            checkForUpdates(showResult = true)
        }
    }

    fun uninstallOldVersion() {
        UpdateInstaller.openUninstall(getApplication())
    }

    fun openReleasePage() {
        UpdateInstaller.openReleasePage(
            getApplication(),
            _uiState.value.update.info?.pageUrl ?: UpdateConfig.RELEASES_PAGE,
        )
    }

    private fun isUpdateNewer(info: UpdateInfo): Boolean {
        return if (info.versionCode > 0) {
            info.versionCode > BuildConfig.VERSION_CODE
        } else {
            isNewerVersion(info.versionName, BuildConfig.VERSION_NAME)
        }
    }

    private fun updateDir(): File = File(getApplication<Application>().filesDir, "updates")

    @Suppress("DEPRECATION")
    private fun isOnWifi(): Boolean {
        val manager = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    override fun onCleared() {
        updateJob?.cancel()
        streamJob?.cancel()
        super.onCleared()
    }
}
