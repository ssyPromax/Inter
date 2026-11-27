package com.inter.calc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardHide
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inter.calc.ui.theme.LocalInterPalette

enum class KeypadTab(val title: String) {
    MAIN("123"),
    TRIG("三角"),
    FUNC("f(x)"),
}

private enum class KeyKind { NORMAL, NUMBER, ACTION, ACCENT, DANGER }

private data class MathKey(
    val label: String,
    val token: String,
    val kind: KeyKind = KeyKind.NORMAL,
)

// GeoGebra 风格主键盘：数字 + 常用变量 + 智能分数线(a/b) + 幂/根式 + 左右光标
private val mainKeys = listOf(
    MathKey("x", "x"), MathKey("π", "π"), MathKey("7", "7", KeyKind.NUMBER), MathKey("8", "8", KeyKind.NUMBER), MathKey("9", "9", KeyKind.NUMBER), MathKey("÷", "/", KeyKind.ACTION),
    MathKey("x²", "^2"), MathKey("xⁿ", "^("), MathKey("4", "4", KeyKind.NUMBER), MathKey("5", "5", KeyKind.NUMBER), MathKey("6", "6", KeyKind.NUMBER), MathKey("×", "*", KeyKind.ACTION),
    MathKey("√", "√("), MathKey("a/b", "__FRAC__", KeyKind.ACCENT), MathKey("1", "1", KeyKind.NUMBER), MathKey("2", "2", KeyKind.NUMBER), MathKey("3", "3", KeyKind.NUMBER), MathKey("-", "-", KeyKind.ACTION),
    MathKey("(", "("), MathKey(")", ")"), MathKey("0", "0", KeyKind.NUMBER), MathKey(".", ".", KeyKind.NUMBER), MathKey("⌫", "__BACKSPACE__", KeyKind.ACTION), MathKey("+", "+", KeyKind.ACTION),
    MathKey("|x|", "|"), MathKey("e", "e"), MathKey("←", "__LEFT__", KeyKind.ACTION), MathKey("→", "__RIGHT__", KeyKind.ACTION), MathKey("清空", "__CLEAR__", KeyKind.DANGER), MathKey("eˣ", "e^("),
)

// 三角/双曲/反双曲界面：加入 arsh、arch、arth，移除 sin² 与 cos²
private val trigKeys = listOf(
    MathKey("sin", "sin("), MathKey("cos", "cos("), MathKey("tan", "tan("), MathKey("cot", "cot("), MathKey("⌫", "__BACKSPACE__", KeyKind.ACTION),
    MathKey("sec", "sec("), MathKey("csc", "csc("), MathKey("asin", "asin("), MathKey("acos", "acos("), MathKey("atan", "atan("),
    MathKey("sinh", "sinh("), MathKey("cosh", "cosh("), MathKey("tanh", "tanh("), MathKey("arsh", "arsh("), MathKey("arch", "arch("),
    MathKey("arth", "arth("), MathKey("x", "x"), MathKey("π", "π"), MathKey("(", "("), MathKey(")", ")"),
    MathKey("a/b", "__FRAC__", KeyKind.ACCENT), MathKey("^", "^"), MathKey("←", "__LEFT__", KeyKind.ACTION), MathKey("→", "__RIGHT__", KeyKind.ACTION), MathKey("清空", "__CLEAR__", KeyKind.DANGER),
)

// 常用函数界面（无预置分数文本框，保留简洁高效的函数键）
private val funcKeys = listOf(
    MathKey("ln", "ln("), MathKey("log", "log("), MathKey("eˣ", "e^("), MathKey("2ˣ", "2^("), MathKey("⌫", "__BACKSPACE__", KeyKind.ACTION),
    MathKey("√", "√("), MathKey("∛", "cbrt("), MathKey("|x|", "|"), MathKey("x²", "^2"), MathKey("x³", "^3"),
    MathKey("a/b", "__FRAC__", KeyKind.ACCENT), MathKey("xⁿ", "^("), MathKey("e", "e"), MathKey("π", "π"), MathKey("x", "x"),
    MathKey("+", "+"), MathKey("-", "-"), MathKey("×", "*"), MathKey("÷", "/"), MathKey("(", "("),
    MathKey(")", ")"), MathKey("←", "__LEFT__", KeyKind.ACTION), MathKey("→", "__RIGHT__", KeyKind.ACTION), MathKey("0", "0"), MathKey("清空", "__CLEAR__", KeyKind.DANGER),
)

@Composable
fun MathKeypad(
    onInsert: (String) -> Unit,
    onInsertFraction: () -> Unit,
    onMoveCursor: (Int) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onHideKeypad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalInterPalette.current
    var activeTab by remember { mutableStateOf(KeypadTab.MAIN) }

    val (keys, columns) = when (activeTab) {
        KeypadTab.MAIN -> mainKeys to 6
        KeypadTab.TRIG -> trigKeys to 5
        KeypadTab.FUNC -> funcKeys to 5
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.background)
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // GeoGebra 风格顶部标签栏（123 | 三角 | f(x) + 右侧收起键）
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeypadTab.entries.forEach { tab ->
                    val selected = tab == activeTab
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { activeTab = tab },
                        shape = RoundedCornerShape(50),
                        color = if (selected) palette.selectedCard else palette.card,
                        border = if (selected) null else BorderStroke(1.dp, palette.border),
                    ) {
                        Text(
                            text = tab.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) palette.onSelected else palette.textSecondary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onHideKeypad),
                shape = RoundedCornerShape(10.dp),
                color = palette.card,
                border = BorderStroke(1.dp, palette.border),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardHide,
                        contentDescription = "收起键盘",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }

        // 按键网格
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            keys.chunked(columns).forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    rowKeys.forEach { key ->
                        val bg = when (key.kind) {
                            KeyKind.DANGER -> palette.danger.copy(alpha = 0.13f)
                            KeyKind.ACCENT -> palette.accentSoft
                            KeyKind.ACTION -> palette.border.copy(alpha = 0.55f)
                            KeyKind.NUMBER -> palette.card
                            KeyKind.NORMAL -> palette.card
                        }
                        val fg = when (key.kind) {
                            KeyKind.DANGER -> palette.danger
                            KeyKind.ACCENT -> palette.accent
                            KeyKind.ACTION -> palette.textPrimary
                            KeyKind.NUMBER -> palette.textPrimary
                            KeyKind.NORMAL -> palette.textPrimary
                        }
                        val border = if (key.kind == KeyKind.NUMBER || key.kind == KeyKind.NORMAL) {
                            BorderStroke(1.dp, palette.border)
                        } else {
                            null
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    when (key.token) {
                                        "__BACKSPACE__" -> onBackspace()
                                        "__CLEAR__" -> onClear()
                                        "__FRAC__" -> onInsertFraction()
                                        "__LEFT__" -> onMoveCursor(-1)
                                        "__RIGHT__" -> onMoveCursor(1)
                                        else -> onInsert(key.token)
                                    }
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = bg,
                            border = border,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when (key.token) {
                                    "__BACKSPACE__" -> Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                        contentDescription = "退格",
                                        tint = fg,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    "__LEFT__" -> Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                        contentDescription = "左移光标",
                                        tint = fg,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    "__RIGHT__" -> Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = "右移光标",
                                        tint = fg,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    "__FRAC__" -> Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text("□", fontSize = 10.sp, color = fg, lineHeight = 10.sp)
                                        Box(modifier = Modifier.width(14.dp).height(1.4.dp).background(fg))
                                        Text("□", fontSize = 10.sp, color = fg, lineHeight = 10.sp)
                                    }
                                    else -> Text(
                                        text = key.label,
                                        fontSize = if (key.label.length >= 4) 12.5.sp else 15.sp,
                                        fontWeight = if (key.kind == KeyKind.NUMBER) FontWeight.Medium else FontWeight.SemiBold,
                                        color = fg,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
