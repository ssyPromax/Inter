package com.inter.calc.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inter.calc.math.InputLatexFormatter
import com.inter.calc.ui.components.AlgebraCard
import com.inter.calc.ui.components.ApiSettingsDialog
import com.inter.calc.ui.components.DeepSeekExplainScreen
import com.inter.calc.ui.components.FunctionGraphCard
import com.inter.calc.ui.components.LatexFormulaView
import com.inter.calc.ui.components.MathKeypad
import com.inter.calc.ui.components.UpdateBanner
import com.inter.calc.ui.components.UpdateDialog
import com.inter.calc.ui.theme.LocalInterPalette

@Composable
fun HomeScreen(viewModel: InterViewModel) {
    val state by viewModel.uiState.collectAsState()
    val palette = LocalInterPalette.current

    if (state.showExplainScreen) {
        DeepSeekExplainScreen(
            functionLatex = state.originalLatex.ifBlank { state.input.text },
            messages = state.explainMessages,
            isStreaming = state.isStreamingExplain,
            errorMessage = state.explainError,
            hasApiKey = state.deepSeekConfig.isConfigured,
            onSendQuestion = viewModel::sendFollowUpQuestion,
            onRetryInitial = viewModel::startInitialExplanation,
            onOpenApiSettings = viewModel::openApiSettings,
            onBack = viewModel::closeExplainScreen,
        )
    } else {
        val accentHex = remember(palette.accent) {
            String.format("#%06X", 0xFFFFFF and palette.accent.toArgb())
        }
        val liveInputLatex = remember(state.input.text, state.input.selection, accentHex) {
            if (state.input.text.isEmpty()) ""
            else InputLatexFormatter.toLiveLatex(state.input.text, state.input.selection.end, accentHex)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
        ) {
            // 上半区：GeoGebra 风格全宽实时绘图区（含曲线旁表达式标注）
            FunctionGraphCard(
                originalPoints = state.originalPoints,
                derivativePoints = state.derivativePoints,
                integralPoints = state.integralPoints,
                originalLabel = state.originalLabel,
                derivativeLabel = state.derivativeLabel,
                integralLabel = state.integralLabel,
                xCenter = state.xCenter,
                yCenter = state.yCenter,
                xHalfSpan = state.xHalfSpan,
                showOriginal = state.showOriginal,
                showDerivative = state.showDerivative,
                showIntegral = state.showIntegral,
                darkTheme = state.darkTheme,
                apiConfigured = state.deepSeekConfig.isConfigured,
                onToggleOriginal = viewModel::toggleOriginalCurve,
                onToggleDerivative = viewModel::toggleDerivativeCurve,
                onToggleIntegral = viewModel::toggleIntegralCurve,
                onToggleTheme = viewModel::toggleDarkTheme,
                onOpenApiSettings = viewModel::openApiSettings,
                onTransformViewport = viewModel::transformViewport,
                onResetViewport = viewModel::resetViewport,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (state.showKeypad) 0.40f else 0.56f),
            )

            // 中段：圆角面板（LaTeX 实时输入框 + LaTeX 代数区）
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (state.showKeypad) 0.32f else 0.44f),
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                color = palette.card,
                border = BorderStroke(1.dp, palette.border),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(34.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(palette.border)
                        )
                    }

                    if (state.update.showBanner) {
                        UpdateBanner(
                            update = state.update,
                            onOpen = viewModel::openUpdateDialog,
                            onDismiss = viewModel::dismissUpdateBanner,
                            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                        )
                    }

                    // 实时 LaTeX 格式输入框（点击唤起底部快捷键盘，带绿色光标竖线）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 42.dp)
                            .clickable { viewModel.setKeypadVisible(true) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(22.dp),
                        )
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (state.input.text.isEmpty()) {
                                Text(
                                    text = "输入函数…",
                                    fontSize = 16.sp,
                                    color = palette.textSecondary.copy(alpha = 0.7f),
                                )
                            } else {
                                LatexFormulaView(
                                    latex = liveInputLatex,
                                    textColor = palette.textPrimary,
                                    fontSizePx = 18,
                                )
                            }
                        }

                        if (state.input.text.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "清空",
                                tint = palette.textSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable { viewModel.clearInput() },
                            )
                        }

                        if (!state.showKeypad) {
                            Surface(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .clickable { viewModel.setKeypadVisible(true) },
                                shape = CircleShape,
                                color = palette.accentSoft,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Keyboard,
                                        contentDescription = "展开键盘",
                                        tint = palette.accent,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = palette.border, thickness = 1.dp)

                    // 代数区（可滚动）
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(top = 8.dp, bottom = 10.dp),
                    ) {
                        AlgebraCard(
                            originalLatex = state.originalLatex,
                            derivativeLatex = state.derivativeLatex,
                            integralLatex = state.integralLatex,
                            integralIsNumericOnly = state.integralIsNumericOnly,
                            errorMessage = state.parseError,
                            onExplainIntegral = viewModel::openExplainScreen,
                        )
                    }
                }
            }

            // 下半区：固定底部 GeoGebra 风格快捷键盘
            AnimatedVisibility(visible = state.showKeypad) {
                MathKeypad(
                    onInsert = viewModel::insertToken,
                    onInsertFraction = viewModel::insertFraction,
                    onMoveCursor = viewModel::moveCursor,
                    onBackspace = viewModel::backspace,
                    onClear = viewModel::clearInput,
                    onHideKeypad = { viewModel.setKeypadVisible(false) },
                )
            }
        }
    }

    if (state.showApiDialog) {
        ApiSettingsDialog(
            initialConfig = state.deepSeekConfig,
            currentVersion = state.update.currentVersion,
            onSave = viewModel::saveApiSettings,
            onDismiss = viewModel::closeApiSettings,
            onCheckUpdate = viewModel::openUpdateFromSettings,
        )
    }

    if (state.update.showDialog) {
        UpdateDialog(
            update = state.update,
            onDismiss = viewModel::dismissUpdateDialog,
            onDownload = viewModel::downloadUpdate,
            onInstall = viewModel::installUpdate,
            onRetry = viewModel::retryUpdate,
            onUninstall = viewModel::uninstallOldVersion,
            onOpenReleasePage = viewModel::openReleasePage,
        )
    }
}
