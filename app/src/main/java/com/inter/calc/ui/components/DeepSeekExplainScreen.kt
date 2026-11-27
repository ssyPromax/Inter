package com.inter.calc.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inter.calc.data.ChatMessage
import com.inter.calc.ui.theme.LocalInterPalette

private val quickQuestions = listOf(
    "展开第一步推导",
    "换一种更简方法",
    "总结此类通法",
)

/**
 * 独立全屏的 DeepSeek 积分方法讲解与追问界面（替代半屏小窗）。
 */
@Composable
fun DeepSeekExplainScreen(
    functionLatex: String,
    messages: List<ChatMessage>,
    isStreaming: Boolean,
    errorMessage: String?,
    hasApiKey: Boolean,
    onSendQuestion: (String) -> Unit,
    onRetryInitial: () -> Unit,
    onOpenApiSettings: () -> Unit,
    onBack: () -> Unit,
) {
    val palette = LocalInterPalette.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    BackHandler(onBack = onBack)

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // 顶部全屏导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onBack),
                    shape = CircleShape,
                    color = palette.card,
                    border = BorderStroke(1.dp, palette.border),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    text = "积分思路",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier.size(36.dp).clip(CircleShape).clickable(enabled = !isStreaming, onClick = onRetryInitial),
                    shape = CircleShape,
                    color = palette.card,
                    border = BorderStroke(1.dp, palette.border),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Refresh, "重新生成", tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }
                Surface(
                    modifier = Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onOpenApiSettings),
                    shape = CircleShape,
                    color = palette.card,
                    border = BorderStroke(1.dp, palette.border),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Key, "API 设置", tint = if (hasApiKey) palette.accent else palette.danger, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // 顶部题干卡片（LaTeX 全宽展示）
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border),
        ) {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                LatexFormulaView(
                    latex = "\\int \\left($functionLatex\\right) dx",
                    textColor = palette.textPrimary,
                    fontSizePx = 16,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (!hasApiKey) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = palette.card,
                border = BorderStroke(1.dp, palette.border),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("尚未配置 DeepSeek API Key", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
                    Text(
                        "填写后即可调用 DeepSeek 以多行 LaTeX 给出常用积分方法，并支持追问。",
                        fontSize = 13.5.sp,
                        color = palette.textSecondary,
                    )
                    Button(
                        onClick = onOpenApiSettings,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                    ) {
                        Text("填写 DeepSeek API Key", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        } else {
            // 全屏对话区域
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                items(messages) { msg ->
                    val isUser = msg.role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                    ) {
                        if (isUser) {
                            Surface(
                                modifier = Modifier.widthIn(max = 300.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = palette.selectedCard,
                            ) {
                                Text(
                                    text = msg.content,
                                    fontSize = 13.5.sp,
                                    color = palette.onSelected,
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                                )
                            }
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = palette.card,
                                border = BorderStroke(1.dp, palette.border),
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                                    if (msg.content.isEmpty()) {
                                        Text("正在推导…", fontSize = 14.sp, color = palette.textSecondary)
                                    } else {
                                        LatexMarkdownView(
                                            markdown = msg.content,
                                            textColor = palette.textPrimary,
                                            accentColor = palette.accent,
                                            fontSizePx = 15,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = palette.danger.copy(alpha = 0.12f),
                        ) {
                            Text(errorMessage, fontSize = 13.sp, color = palette.danger, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }

            // 快捷追问胶囊 + 底部输入框
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickQuestions) { q ->
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(50)).clickable(enabled = !isStreaming) { onSendQuestion(q) },
                            shape = RoundedCornerShape(50),
                            color = palette.card,
                            border = BorderStroke(1.dp, palette.border),
                        ) {
                            Text(q, fontSize = 12.sp, color = palette.textSecondary, modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("有不懂的直接问…", fontSize = 14.sp, color = palette.textSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = palette.card,
                            unfocusedContainerColor = palette.card,
                            focusedBorderColor = palette.accent,
                            unfocusedBorderColor = palette.border,
                            focusedTextColor = palette.textPrimary,
                            unfocusedTextColor = palette.textPrimary,
                        ),
                    )
                    Surface(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isStreaming && inputText.isNotBlank()) {
                                val q = inputText.trim()
                                inputText = ""
                                onSendQuestion(q)
                            },
                        shape = CircleShape,
                        color = if (!isStreaming && inputText.isNotBlank()) palette.accent else palette.border,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isStreaming) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(Icons.AutoMirrored.Rounded.Send, "发送", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
