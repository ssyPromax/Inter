package com.inter.calc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inter.calc.ui.theme.LocalInterPalette

/**
 * 代数区：每条函数占满全宽展示完整 LaTeX，积分行将「积分思路」按钮置于标题行右侧，
 * 避免长积分结果被按钮挤压截断。
 */
@Composable
fun AlgebraCard(
    originalLatex: String,
    derivativeLatex: String,
    integralLatex: String,
    integralIsNumericOnly: Boolean,
    errorMessage: String?,
    onExplainIntegral: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalInterPalette.current

    if (errorMessage != null) {
        Text(
            text = errorMessage,
            fontSize = 12.5.sp,
            color = palette.danger,
            modifier = modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        )
        return
    }

    if (originalLatex.isBlank()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = palette.card,
        border = BorderStroke(1.dp, palette.border),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 原函数 f(x) - 黑色
            LatexAlgebraRow(
                dotColor = palette.curveOriginal,
                latex = "f(x) = $originalLatex",
            )
            HorizontalDivider(color = palette.border, thickness = 1.dp)

            // 导数 f'(x) - 蓝色
            LatexAlgebraRow(
                dotColor = palette.curveDerivative,
                latex = "f'(x) = $derivativeLatex",
            )
            HorizontalDivider(color = palette.border, thickness = 1.dp)

            // 积分 ∫f(x)dx - 红色（标题与「积分思路」按钮同行，公式独享整行全宽！）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(palette.curveIntegral)
                        )
                        Text(
                            text = if (integralIsNumericOnly) "不定积分（图像已数值积分）" else "不定积分",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textSecondary,
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onExplainIntegral),
                        shape = RoundedCornerShape(50),
                        color = palette.accentSoft,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "DeepSeek 积分思路",
                                tint = palette.accent,
                                modifier = Modifier.size(13.5.dp),
                            )
                            Text(
                                text = "积分思路",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.accent,
                            )
                        }
                    }
                }

                LatexFormulaView(
                    latex = if (integralIsNumericOnly) {
                        "\\int f(x)\\,dx \\;\\text{（无初等原函数）}"
                    } else {
                        "\\int f(x)\\,dx = $integralLatex + C"
                    },
                    textColor = palette.textPrimary,
                    fontSizePx = 15,
                    modifier = Modifier.padding(start = 15.dp),
                )
            }
        }
    }
}

@Composable
private fun LatexAlgebraRow(
    dotColor: Color,
    latex: String,
) {
    val palette = LocalInterPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Box(modifier = Modifier.weight(1f)) {
            LatexFormulaView(
                latex = latex,
                textColor = palette.textPrimary,
                fontSizePx = 15,
            )
        }
    }
}
