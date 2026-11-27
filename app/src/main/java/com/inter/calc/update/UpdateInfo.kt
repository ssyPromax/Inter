package com.inter.calc.update

import java.util.Locale

/** 一次可用更新的描述。 */
data class UpdateInfo(
    val versionName: String,
    val versionCode: Int = 0,
    val notes: String = "",
    val apkUrl: String,
    val assetName: String,
    val pageUrl: String = "",
    val publishedAt: String = "",
    val sha256: String? = null,
    val sizeBytes: Long = 0L,
)

class UpdateException(message: String) : Exception(message)

/** 把 1.2.1 / v1.2.1 / 1.2.1-beta 解析成数字段。 */
fun versionParts(version: String): List<Int> {
    val cleaned = version.trim().removePrefix("v").removePrefix("V")
    val core = cleaned.substringBefore('-').substringBefore('+')
    if (core.isBlank()) return emptyList()
    return core.split('.', '_', ' ').map { part ->
        part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
    }
}

/** 逐段比较版本号；返回正数表示 a 更新。 */
fun compareVersions(a: String, b: String): Int {
    val left = versionParts(a)
    val right = versionParts(b)
    val size = maxOf(left.size, right.size)
    for (i in 0 until size) {
        val x = left.getOrElse(i) { 0 }
        val y = right.getOrElse(i) { 0 }
        if (x != y) return x.compareTo(y)
    }
    return 0
}

fun isNewerVersion(latest: String, current: String): Boolean = compareVersions(latest, current) > 0

/** Release 正文是 Markdown，弹窗里只展示纯文本。 */
fun cleanReleaseNotes(raw: String): String {
    if (raw.isBlank()) return "本次更新包含若干修复与改进。"
    val cleaned = raw
        .replace(Regex("```[\\s\\S]*?```")) { it.value.replace("```", "").trim() }
        .replace(Regex("(?m)^#{1,6}\\s*"), "")
        .replace(Regex("(?m)^\\s*[-*+]\\s+"), "• ")
        .replace(Regex("(?m)^>\\s?"), "")
        .replace(Regex("\\[(.*?)]\\(.*?\\)"), "\$1")
        .replace("**", "")
        .replace("__", "")
        .replace("`", "")
        .trim()
    return cleaned.ifBlank { "本次更新包含若干修复与改进。" }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return ""
    return String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
}
