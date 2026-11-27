package com.inter.calc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inter.calc.R
import com.inter.calc.math.CurvePoint
import com.inter.calc.ui.theme.LocalInterPalette
import kotlin.math.*

/**
 * GeoGebra 风格全宽坐标系画布：
 * - 双层网格 + 带箭头坐标轴
 * - 自适应高精度曲线绘制
 * - 在每条函数图像旁标注对应的函数表达式（黑=f(x)，蓝=f'(x)，红=∫f(x)dx）
 */
@Composable
fun FunctionGraphCard(
    originalPoints: List<CurvePoint>,
    derivativePoints: List<CurvePoint>,
    integralPoints: List<CurvePoint>,
    originalLabel: String,
    derivativeLabel: String,
    integralLabel: String,
    xCenter: Double,
    yCenter: Double,
    xHalfSpan: Double,
    showOriginal: Boolean,
    showDerivative: Boolean,
    showIntegral: Boolean,
    darkTheme: Boolean,
    apiConfigured: Boolean,
    onToggleOriginal: () -> Unit,
    onToggleDerivative: () -> Unit,
    onToggleIntegral: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenApiSettings: () -> Unit,
    onTransformViewport: (dxFrac: Double, dyFrac: Double, zoomFactor: Double) -> Unit,
    onResetViewport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalInterPalette.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.graphCanvas)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val w = size.width.toDouble().coerceAtLeast(1.0)
                    val h = size.height.toDouble().coerceAtLeast(1.0)
                    onTransformViewport(pan.x / w, pan.y / h, zoom.toDouble())
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            val aspect = height / width
            val xMin = xCenter - xHalfSpan
            val xMax = xCenter + xHalfSpan
            val yHalfSpan = xHalfSpan * aspect
            val yMin = yCenter - yHalfSpan
            val yMax = yCenter + yHalfSpan

            fun toScreenX(x: Double): Float = ((x - xMin) / (xMax - xMin) * width).toFloat()
            fun toScreenY(y: Double): Float = ((yMax - y) / (yMax - yMin) * height).toFloat()

            val majorStep = niceGridStep(xMax - xMin)
            val minorStep = majorStep / 5.0
            val minorColor = palette.graphGrid.copy(alpha = 0.55f)
            val majorColor = palette.graphGrid

            var mx = ceil(xMin / minorStep) * minorStep
            while (mx <= xMax + 1e-9) {
                val sx = toScreenX(mx)
                drawLine(minorColor, Offset(sx, 0f), Offset(sx, height), 0.6.dp.toPx())
                mx += minorStep
            }
            var my = ceil(yMin / minorStep) * minorStep
            while (my <= yMax + 1e-9) {
                val sy = toScreenY(my)
                drawLine(minorColor, Offset(0f, sy), Offset(width, sy), 0.6.dp.toPx())
                my += minorStep
            }

            val startX = ceil(xMin / majorStep) * majorStep
            var gx = startX
            while (gx <= xMax + 1e-9) {
                val sx = toScreenX(gx)
                drawLine(majorColor, Offset(sx, 0f), Offset(sx, height), 1.1.dp.toPx())
                gx += majorStep
            }
            val startY = ceil(yMin / majorStep) * majorStep
            var gy = startY
            while (gy <= yMax + 1e-9) {
                val sy = toScreenY(gy)
                drawLine(majorColor, Offset(0f, sy), Offset(width, sy), 1.1.dp.toPx())
                gy += majorStep
            }

            val axisColor = Color(0xFF423D4D)
            val axisY = toScreenY(0.0)
            if (axisY in 0f..height) {
                drawLine(axisColor, Offset(0f, axisY), Offset(width, axisY), 1.4.dp.toPx())
                val arr = 6.dp.toPx()
                drawLine(axisColor, Offset(width - arr, axisY - arr * 0.7f), Offset(width - 1f, axisY), 1.4.dp.toPx())
                drawLine(axisColor, Offset(width - arr, axisY + arr * 0.7f), Offset(width - 1f, axisY), 1.4.dp.toPx())
            }
            val axisX = toScreenX(0.0)
            if (axisX in 0f..width) {
                drawLine(axisColor, Offset(axisX, 0f), Offset(axisX, height), 1.4.dp.toPx())
                val arr = 6.dp.toPx()
                drawLine(axisColor, Offset(axisX - arr * 0.7f, arr), Offset(axisX, 1f), 1.4.dp.toPx())
                drawLine(axisColor, Offset(axisX + arr * 0.7f, arr), Offset(axisX, 1f), 1.4.dp.toPx())
            }

            val tickPaint = android.graphics.Paint().apply {
                color = axisColor.toArgb()
                textSize = 10.5.sp.toPx()
                isAntiAlias = true
            }
            val clampedAxisY = axisY.coerceIn(18.dp.toPx(), height - 8.dp.toPx())
            val clampedAxisX = axisX.coerceIn(8.dp.toPx(), width - 30.dp.toPx())

            gx = startX
            while (gx <= xMax + 1e-9) {
                if (abs(gx) > majorStep * 0.2) {
                    val sx = toScreenX(gx)
                    if (sx in 18f..(width - 18f)) {
                        drawContext.canvas.nativeCanvas.drawText(formatTick(gx), sx - 4.dp.toPx(), clampedAxisY + 13.dp.toPx(), tickPaint)
                    }
                }
                gx += majorStep
            }
            gy = startY
            while (gy <= yMax + 1e-9) {
                if (abs(gy) > majorStep * 0.2) {
                    val sy = toScreenY(gy)
                    if (sy in 18f..(height - 14f)) {
                        drawContext.canvas.nativeCanvas.drawText(formatTick(gy), clampedAxisX - 20.dp.toPx(), sy + 3.5.dp.toPx(), tickPaint)
                    }
                }
                gy += majorStep
            }

            val maxJump = (yMax - yMin) * 0.75
            if (showIntegral) drawCurve(integralPoints, palette.curveIntegral, 2.0.dp.toPx(), maxJump, yMin, yMax, ::toScreenX, ::toScreenY)
            if (showDerivative) drawCurve(derivativePoints, palette.curveDerivative, 2.0.dp.toPx(), maxJump, yMin, yMax, ::toScreenX, ::toScreenY)
            if (showOriginal) drawCurve(originalPoints, palette.curveOriginal, 2.4.dp.toPx(), maxJump, yMin, yMax, ::toScreenX, ::toScreenY)

            // 在每条可见曲线旁边标注对应的函数表达式（带半透明圆角底避免与网格重叠）
            val placedRects = mutableListOf<android.graphics.RectF>()
            val pillBgColor = palette.graphCanvas.copy(alpha = 0.86f).toArgb()

            if (showOriginal && originalLabel.isNotBlank()) {
                drawCurveLabel(
                    points = originalPoints,
                    text = "f(x) = $originalLabel",
                    color = palette.curveOriginal,
                    preferredFrac = 0.62f,
                    pillBgColor = pillBgColor,
                    placedRects = placedRects,
                    toScreenX = ::toScreenX,
                    toScreenY = ::toScreenY,
                )
            }
            if (showDerivative && derivativeLabel.isNotBlank()) {
                drawCurveLabel(
                    points = derivativePoints,
                    text = "f'(x) = $derivativeLabel",
                    color = palette.curveDerivative,
                    preferredFrac = 0.30f,
                    pillBgColor = pillBgColor,
                    placedRects = placedRects,
                    toScreenX = ::toScreenX,
                    toScreenY = ::toScreenY,
                )
            }
            if (showIntegral && integralLabel.isNotBlank()) {
                drawCurveLabel(
                    points = integralPoints,
                    text = "∫f = $integralLabel",
                    color = palette.curveIntegral,
                    preferredFrac = 0.75f,
                    pillBgColor = pillBgColor,
                    placedRects = placedRects,
                    toScreenX = ::toScreenX,
                    toScreenY = ::toScreenY,
                )
            }
        }

        // 顶部悬浮栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_inter_logo),
                    contentDescription = "Inter",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)),
                )
                CurveLegendPill("f", palette.curveOriginal, showOriginal, onToggleOriginal)
                CurveLegendPill("f'", palette.curveDerivative, showDerivative, onToggleDerivative)
                CurveLegendPill("∫", palette.curveIntegral, showIntegral, onToggleIntegral)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onOpenApiSettings),
                    shape = RoundedCornerShape(50),
                    color = if (apiConfigured) palette.accentSoft.copy(alpha = 0.94f) else palette.card.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, if (apiConfigured) palette.accent.copy(alpha = 0.4f) else palette.border),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = "DS API",
                            tint = if (apiConfigured) palette.accent else palette.textSecondary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = if (apiConfigured) "DS" else "API",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (apiConfigured) palette.accent else palette.textPrimary,
                        )
                    }
                }

                FloatingIconChip(onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (darkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = "切换主题",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(17.dp),
                    )
                }

                FloatingIconChip(onClick = onResetViewport) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "重置视角",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCurveLabel(
    points: List<CurvePoint>,
    text: String,
    color: Color,
    preferredFrac: Float,
    pillBgColor: Int,
    placedRects: MutableList<android.graphics.RectF>,
    toScreenX: (Double) -> Float,
    toScreenY: (Double) -> Float,
) {
    if (points.isEmpty()) return
    val displayText = if (text.length > 26) text.take(25) + "…" else text
    val topSafe = 68.dp.toPx()
    val bottomSafe = size.height - 20.dp.toPx()
    val sideSafe = 12.dp.toPx()
    val targetX = size.width * preferredFrac

    // 寻找最靠近 preferredFrac 且位于画布安全可见区域内的曲线点
    var bestScreenX = Float.NaN
    var bestScreenY = Float.NaN
    var bestScore = Float.MAX_VALUE

    for (pt in points) {
        if (!pt.y.isFinite()) continue
        val sx = toScreenX(pt.x)
        val sy = toScreenY(pt.y)
        if (sx in (sideSafe + 10f)..(size.width - sideSafe - 10f) && sy in topSafe..bottomSafe) {
            val score = abs(sx - targetX)
            if (score < bestScore) {
                bestScore = score
                bestScreenX = sx
                bestScreenY = sy
            }
        }
    }
    if (!bestScreenX.isFinite() || !bestScreenY.isFinite()) return

    val textPaint = android.graphics.Paint().apply {
        this.color = color.toArgb()
        textSize = 11.5.sp.toPx()
        isAntiAlias = true
        isFakeBoldText = true
    }
    val textW = textPaint.measureText(displayText)
    val textH = 14.dp.toPx()
    val padH = 5.dp.toPx()
    val padV = 3.dp.toPx()

    var lx = (bestScreenX + 8.dp.toPx()).coerceIn(sideSafe, (size.width - textW - padH * 2 - sideSafe).coerceAtLeast(sideSafe))
    var ly = (bestScreenY - 12.dp.toPx()).coerceIn(topSafe, (bottomSafe - textH - padV * 2).coerceAtLeast(topSafe))

    var rect = android.graphics.RectF(lx, ly, lx + textW + padH * 2, ly + textH + padV * 2)
    // 避让已放置的标签，避免互相遮挡
    var attempts = 0
    while (attempts < 4 && placedRects.any { android.graphics.RectF.intersects(it, rect) }) {
        ly = (ly + textH + padV * 2 + 6.dp.toPx()).coerceAtMost(bottomSafe - textH - padV * 2)
        rect = android.graphics.RectF(lx, ly, lx + textW + padH * 2, ly + textH + padV * 2)
        attempts++
    }
    placedRects.add(rect)

    val bgPaint = android.graphics.Paint().apply {
        this.color = pillBgColor
        isAntiAlias = true
    }
    val borderPaint = android.graphics.Paint().apply {
        this.color = color.copy(alpha = 0.35f).toArgb()
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 1.dp.toPx()
        isAntiAlias = true
    }
    val radius = 6.dp.toPx()
    drawContext.canvas.nativeCanvas.drawRoundRect(rect, radius, radius, bgPaint)
    drawContext.canvas.nativeCanvas.drawRoundRect(rect, radius, radius, borderPaint)
    drawContext.canvas.nativeCanvas.drawText(displayText, rect.left + padH, rect.bottom - padV - 2.dp.toPx(), textPaint)
}

@Composable
private fun FloatingIconChip(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val palette = LocalInterPalette.current
    Surface(
        modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onClick),
        shape = CircleShape,
        color = palette.card.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, palette.border),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun CurveLegendPill(
    label: String,
    dotColor: Color,
    active: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalInterPalette.current
    Surface(
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = if (active) palette.card.copy(alpha = 0.92f) else palette.card.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, if (active) palette.border else palette.border.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier.size(7.dp).clip(CircleShape).background(if (active) dotColor else dotColor.copy(alpha = 0.25f))
            )
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (active) palette.textPrimary else palette.textSecondary,
            )
        }
    }
}

private fun DrawScope.drawCurve(
    points: List<CurvePoint>,
    color: Color,
    strokeWidth: Float,
    maxJump: Double,
    yMin: Double,
    yMax: Double,
    toScreenX: (Double) -> Float,
    toScreenY: (Double) -> Float,
) {
    if (points.size < 2) return
    val path = Path()
    var drawing = false
    var prevY = Double.NaN
    val clipMargin = (yMax - yMin) * 4.0

    for (pt in points) {
        val valid = pt.y.isFinite() && pt.y in (yMin - clipMargin)..(yMax + clipMargin)
        if (!valid) {
            drawing = false
            prevY = Double.NaN
            continue
        }
        val sx = toScreenX(pt.x)
        val sy = toScreenY(pt.y)
        if (!drawing || (prevY.isFinite() && abs(pt.y - prevY) > maxJump)) {
            path.moveTo(sx, sy)
            drawing = true
        } else {
            path.lineTo(sx, sy)
        }
        prevY = pt.y
    }

    drawPath(path = path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun niceGridStep(span: Double): Double {
    val rough = span / 6.0
    val pow10 = 10.0.pow(floor(log10(rough.coerceAtLeast(1e-6))))
    val norm = rough / pow10
    val step = when {
        norm < 1.5 -> 1.0
        norm < 3.5 -> 2.0
        norm < 7.5 -> 5.0
        else -> 10.0
    }
    return step * pow10
}

private fun formatTick(v: Double): String {
    val r = round(v)
    return if (abs(v - r) < 1e-6) r.toLong().toString() else "%.1f".format(v)
}
