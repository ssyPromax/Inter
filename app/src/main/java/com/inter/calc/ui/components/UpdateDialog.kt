package com.inter.calc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.inter.calc.ui.UpdatePhase
import com.inter.calc.ui.UpdateUiState
import com.inter.calc.ui.theme.DsRadius
import com.inter.calc.ui.theme.DsSpace
import com.inter.calc.ui.theme.DsType
import com.inter.calc.ui.theme.LocalInterPalette
import com.inter.calc.update.cleanReleaseNotes
import com.inter.calc.update.formatBytes

@Composable
fun UpdateDialog(
    update: UpdateUiState,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onRetry: () -> Unit,
    onUninstall: () -> Unit,
    onOpenReleasePage: () -> Unit,
) {
    val palette = LocalInterPalette.current
    val info = update.info
    val title = when (update.phase) {
        UpdatePhase.CHECKING -> "正在检查更新"
        UpdatePhase.UP_TO_DATE -> "已是最新版本"
        UpdatePhase.ERROR -> if (info != null) "更新 v${info.versionName}" else "检查更新"
        else -> if (info != null) "发现新版本 v${info.versionName}" else "检查更新"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(DsRadius.xxl),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border),
        ) {
            Column(
                modifier = Modifier.padding(DsSpace.xl),
                verticalArrangement = Arrangement.spacedBy(DsSpace.md),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (update.phase == UpdatePhase.UP_TO_DATE) {
                            Icons.Rounded.CheckCircle
                        } else {
                            Icons.Rounded.SystemUpdate
                        },
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.width(DsSpace.sm))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = DsType.title.size,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary,
                        )
                        Text(
                            text = "当前版本 v${update.currentVersion}",
                            fontSize = DsType.caption.size,
                            color = palette.textSecondary,
                        )
                    }
                }

                if (info != null && update.phase != UpdatePhase.UP_TO_DATE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DsSpace.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VersionPill("当前 v${update.currentVersion}", palette.textSecondary, palette.background)
                        Text("→", color = palette.textSecondary, fontSize = DsType.label.size)
                        VersionPill("最新 v${info.versionName}", palette.accent, palette.accentSoft)
                        val sizeText = formatBytes(info.sizeBytes)
                        if (sizeText.isNotBlank()) {
                            Spacer(Modifier.weight(1f))
                            Text(sizeText, color = palette.textSecondary, fontSize = DsType.caption.size)
                        }
                    }
                }

                if (update.phase == UpdatePhase.UP_TO_DATE) {
                    Text(
                        text = "你正在使用最新版本，不需要更新。",
                        fontSize = DsType.body.size,
                        color = palette.textSecondary,
                    )
                }

                if (info != null && update.phase != UpdatePhase.UP_TO_DATE) {
                    Surface(
                        shape = RoundedCornerShape(DsRadius.md),
                        color = palette.background,
                        border = BorderStroke(1.dp, palette.border),
                    ) {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 190.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(DsSpace.md),
                            verticalArrangement = Arrangement.spacedBy(DsSpace.xs),
                        ) {
                            Text(
                                text = "更新内容",
                                fontSize = DsType.label.size,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textSecondary,
                            )
                            Text(
                                text = cleanReleaseNotes(info.notes),
                                fontSize = DsType.body.size,
                                color = palette.textPrimary,
                                lineHeight = DsType.body.size * 1.55f,
                            )
                        }
                    }
                }

                when (update.phase) {
                    UpdatePhase.CHECKING -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = palette.accent,
                            )
                            Spacer(Modifier.width(DsSpace.sm))
                            Text(
                                text = "正在从 GitHub 获取最新版本…",
                                fontSize = DsType.body.size,
                                color = palette.textSecondary,
                            )
                        }
                    }
                    UpdatePhase.DOWNLOADING -> {
                        LinearProgressIndicator(
                            progress = { update.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = palette.accent,
                            trackColor = palette.accentSoft,
                        )
                        Text(
                            text = "正在下载安装包… ${update.percent}%",
                            fontSize = DsType.label.size,
                            color = palette.textSecondary,
                        )
                    }
                    UpdatePhase.ERROR -> {
                        Text(
                            text = update.error ?: "更新失败，请重试。",
                            fontSize = DsType.body.size,
                            color = palette.danger,
                        )
                        if (update.signatureMismatch) {
                            Text(
                                text = "v1.2.0 是另一份签名打的包，Android 不允许直接覆盖安装。" +
                                    "请先卸载旧版（会清除应用内的 DeepSeek Key、主题等设置），再安装新版。" +
                                    "从 v1.2.1 起，以后的新版本可以直接在应用内更新。",
                                fontSize = DsType.label.size,
                                color = palette.textSecondary,
                                lineHeight = DsType.label.size * 1.55f,
                            )
                        }
                    }
                    UpdatePhase.READY -> {
                        Text(
                            text = "安装包已下载并通过校验，点“安装更新”交给系统安装。",
                            fontSize = DsType.body.size,
                            color = palette.textSecondary,
                        )
                        if (!update.error.isNullOrBlank()) {
                            Text(
                                text = update.error,
                                fontSize = DsType.label.size,
                                color = palette.danger,
                            )
                        }
                    }
                    else -> Unit
                }

                if (update.signatureMismatch && update.phase == UpdatePhase.ERROR) {
                    TextButton(onClick = onOpenReleasePage) {
                        Text("打开发布页手动下载", color = palette.accent, fontSize = DsType.label.size)
                    }
                }

                HorizontalDivider(color = palette.border, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when (update.phase) {
                        UpdatePhase.CHECKING -> {
                            Text("请稍候…", fontSize = DsType.label.size, color = palette.textSecondary)
                        }
                        UpdatePhase.UP_TO_DATE -> {
                            TextButton(onClick = onDismiss) {
                                Text("关闭", color = palette.textSecondary)
                            }
                        }
                        UpdatePhase.AVAILABLE -> {
                            TextButton(onClick = onDismiss) {
                                Text("稍后", color = palette.textSecondary)
                            }
                            Spacer(Modifier.width(DsSpace.sm))
                            PrimaryButton(text = "下载更新", onClick = onDownload)
                        }
                        UpdatePhase.DOWNLOADING -> {
                            TextButton(onClick = onDismiss) {
                                Text("后台下载", color = palette.textSecondary)
                            }
                            Spacer(Modifier.width(DsSpace.sm))
                            Text(
                                text = "${update.percent}%",
                                fontSize = DsType.label.size,
                                color = palette.accent,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        UpdatePhase.READY -> {
                            TextButton(onClick = onDismiss) {
                                Text("稍后", color = palette.textSecondary)
                            }
                            Spacer(Modifier.width(DsSpace.sm))
                            PrimaryButton(text = "安装更新", onClick = onInstall)
                        }
                        UpdatePhase.ERROR -> {
                            if (update.signatureMismatch) {
                                TextButton(onClick = onUninstall) {
                                    Text("卸载旧版", color = palette.danger, fontSize = DsType.label.size)
                                }
                                Spacer(Modifier.width(DsSpace.sm))
                                PrimaryButton(text = "打开发布页", onClick = onOpenReleasePage)
                            } else {
                                TextButton(onClick = onDismiss) {
                                    Text("关闭", color = palette.textSecondary)
                                }
                                Spacer(Modifier.width(DsSpace.sm))
                                PrimaryButton(text = "重试", onClick = onRetry)
                            }
                        }
                        else -> {
                            TextButton(onClick = onDismiss) {
                                Text("关闭", color = palette.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionPill(text: String, contentColor: Color, background: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = background,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.3f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = DsSpace.sm, vertical = DsSpace.xs),
            fontSize = DsType.caption.size,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    val palette = LocalInterPalette.current
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(DsRadius.md),
        colors = ButtonDefaults.buttonColors(
            containerColor = palette.accent,
            contentColor = palette.onAccent,
        ),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}
