package com.inter.calc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.inter.calc.ui.UpdatePhase
import com.inter.calc.ui.UpdateUiState
import com.inter.calc.ui.theme.DsRadius
import com.inter.calc.ui.theme.DsSpace
import com.inter.calc.ui.theme.DsType
import com.inter.calc.ui.theme.LocalInterPalette

/**
 * 主界面上的轻量更新提示条：发现新版本、下载进度、下载完成、失败都在这里显示，
 * 不打断用户输入公式。
 */
@Composable
fun UpdateBanner(
    update: UpdateUiState,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalInterPalette.current
    val version = update.info?.versionName.orEmpty()
    val (title, subtitle) = when (update.phase) {
        UpdatePhase.AVAILABLE -> "发现新版本 v$version" to "点击查看更新内容（当前 v${update.currentVersion}）"
        UpdatePhase.DOWNLOADING -> "正在下载 v$version" to "已完成 ${update.percent}%"
        UpdatePhase.READY -> "v$version 已下载" to "点击完成安装"
        UpdatePhase.ERROR -> "更新未完成" to (update.error ?: "点击查看原因")
        else -> return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.md))
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(DsRadius.md),
        color = palette.accentSoft,
        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(
                start = DsSpace.md,
                top = DsSpace.sm,
                end = DsSpace.xs,
                bottom = DsSpace.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.SystemUpdate,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(DsSpace.sm))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DsSpace.xxs)) {
                Text(
                    text = title,
                    fontSize = DsType.body.size,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                )
                Text(
                    text = subtitle,
                    fontSize = DsType.caption.size,
                    color = palette.textSecondary,
                    maxLines = 1,
                )
            }
            if (update.phase == UpdatePhase.DOWNLOADING) {
                Text(
                    text = "${update.percent}%",
                    fontSize = DsType.label.size,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.accent,
                )
                Spacer(Modifier.width(DsSpace.xs))
            }
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "关闭更新提示",
                tint = palette.textSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss)
                    .padding(DsSpace.xxs),
            )
        }
    }
}
