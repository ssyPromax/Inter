package com.inter.calc.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ChatMessage(val role: String, val content: String)

object DeepSeekClient {
    const val SYSTEM_PROMPT = """你是微积分公式导师。所有回复必须极简、清晰、以多行 LaTeX 推导为主，严禁把多个等号连写在一行。
硬性排版规则：
1. 所有公式必须使用 LaTeX：行内用 ${'$'}...${'$'}，推导过程必须使用 ${'$'}${'$'}\begin{aligned} ... \end{aligned}${'$'}${'$'} 换行对齐。
2. 每行最多只能有一个等号 `=`！严禁写出 `A = B = C = D` 这种超长单行公式，必须拆成多行：
${'$'}${'$'}
\begin{aligned}
\int \dots \,dx &= \text{第一步} \\
&= \text{第二步} \\
&= \text{最终结果} + C
\end{aligned}
${'$'}${'$'}
3. 禁止寒暄和长段文字，每个方法只给 1 行简短提示（≤10字，如「令 ${'$'}u=x^2${'$'}」）。
4. 给出 2～3 种最常用方法，标题用 **方法一：方法名**。
5. 若无初等原函数，直接一句话说明并给出级数展开的多行 LaTeX。"""

    fun streamChat(config: DeepSeekConfig, history: List<ChatMessage>): Flow<String> = flow {
        if (!config.isConfigured) throw IllegalStateException("请先在右上角设置中填写 DeepSeek API Key")
        val endpoint = "${config.baseUrl.trimEnd('/')}/chat/completions"
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Authorization", "Bearer ${config.apiKey.trim()}")
            setRequestProperty("Accept", "text/event-stream")
        }
        val messagesArray = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
            for (msg in history) put(JSONObject().put("role", msg.role).put("content", msg.content))
        }
        val payload = JSONObject().apply {
            put("model", config.model)
            put("messages", messagesArray)
            put("stream", true)
            put("temperature", 0.2)
        }
        try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                val errBody = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val readable = when (statusCode) {
                    401 -> "API Key 无效或已过期（401），请检查设置。"
                    402 -> "DeepSeek 账户余额不足（402），请先充值。"
                    429 -> "请求过于频繁（429），请稍后再试。"
                    else -> "请求失败（HTTP $statusCode）：${errBody.take(180)}"
                }
                throw IllegalStateException(readable)
            }
            BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line!!.trim()
                    if (!trimmed.startsWith("data:")) continue
                    val data = trimmed.removePrefix("data:").trim()
                    if (data == "[DONE]") break
                    if (data.isEmpty()) continue
                    val obj = runCatching { JSONObject(data) }.getOrNull() ?: continue
                    val choices = obj.optJSONArray("choices") ?: continue
                    if (choices.length() == 0) continue
                    val delta = choices.getJSONObject(0).optJSONObject("delta") ?: continue
                    val piece = delta.optString("content", "")
                    if (piece.isNotEmpty()) emit(piece)
                }
            }
        } finally {
            connection.disconnect()
        }
    }.flowOn(Dispatchers.IO)
}
