package com.inter.calc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.inter.calc.data.DeepSeekConfig
import com.inter.calc.ui.theme.LocalInterPalette

@Composable
fun ApiSettingsDialog(
    initialConfig: DeepSeekConfig,
    currentVersion: String,
    onSave: (DeepSeekConfig) -> Unit,
    onDismiss: () -> Unit,
    onCheckUpdate: () -> Unit,
) {
    val palette = LocalInterPalette.current
    var apiKey by remember(initialConfig) { mutableStateOf(initialConfig.apiKey) }
    var baseUrl by remember(initialConfig) { mutableStateOf(initialConfig.baseUrl) }
    var model by remember(initialConfig) { mutableStateOf(initialConfig.model) }
    var showKey by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = palette.card,
            border = BorderStroke(1.dp, palette.border),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("DeepSeek API 设置", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
                Text(
                    "Key 仅保存在你的设备本地，用于点击“积分思路”时调用 DeepSeek 讲解常用积分方法并支持追问。",
                    fontSize = 12.5.sp,
                    color = palette.textSecondary,
                    lineHeight = 18.sp,
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("DeepSeek API Key") },
                    placeholder = { Text("sk-...") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "切换显示",
                                tint = palette.textSecondary,
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = palette.accent, unfocusedBorderColor = palette.border),
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("模型选择", fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = palette.textSecondary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeepSeekConfig.AVAILABLE_MODELS.forEach { m ->
                            val selected = model == m
                            Surface(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { model = m },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) palette.selectedCard else palette.background,
                                border = if (selected) null else BorderStroke(1.dp, palette.border),
                            ) {
                                Box(modifier = Modifier.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                                    Text(m, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selected) palette.onSelected else palette.textSecondary)
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("接口地址（默认官方）") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = palette.accent, unfocusedBorderColor = palette.border),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("取消", color = palette.textSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                DeepSeekConfig(
                                    apiKey = apiKey.trim(),
                                    baseUrl = baseUrl.trim().ifBlank { DeepSeekConfig.DEFAULT_BASE_URL },
                                    model = model,
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                    ) {
                        Text("保存", fontWeight = FontWeight.SemiBold)
                    }
                }

                HorizontalDivider(color = palette.border, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Inter v$currentVersion",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textPrimary,
                        )
                        Text(
                            text = "启动时自动检查更新（每 12 小时最多一次）",
                            fontSize = 11.sp,
                            color = palette.textSecondary,
                        )
                    }
                    TextButton(onClick = onCheckUpdate) {
                        Text(
                            text = "检查更新",
                            color = palette.accent,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
