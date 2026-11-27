package com.inter.calc.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

object UpdateClient {

    /**
     * 优先读 Release 里的 update.json（不含 API 限流、带 SHA-256），
     * 读不到再退回 GitHub Releases API。
     */
    suspend fun check(): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            fetchFromFeed()?.let { return@withContext it }
        } catch (_: Exception) {
            // update.json 不可用时继续走 API
        }
        fetchFromApi()
    }

    suspend fun download(
        info: UpdateInfo,
        targetDir: File,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        if (info.apkUrl.isBlank()) throw UpdateException("这个版本没有可下载的安装包。")
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw UpdateException("无法创建更新缓存目录。")
        }

        val safeName = info.assetName
            .ifBlank { "Inter-${info.versionName}.apk" }
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .let { if (it.endsWith(".apk", true)) it else "$it.apk" }
        val target = File(targetDir, safeName)
        val part = File(targetDir, "$safeName.part")
        target.delete()
        part.delete()

        val connection = openConnection(info.apkUrl, "application/octet-stream")
        try {
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                throw UpdateException(
                    if (statusCode == 404) "下载地址失效，请到发布页手动下载。"
                    else "下载失败（HTTP $statusCode）。"
                )
            }
            val total = connection.contentLengthLong.takeIf { it > 0L } ?: info.sizeBytes
            val digest = MessageDigest.getInstance("SHA-256")
            var downloaded = 0L
            var lastPercent = -1

            connection.inputStream.use { input ->
                FileOutputStream(part).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        downloaded += read
                        if (total > 0L) {
                            val percent = ((downloaded * 100L) / total).toInt().coerceIn(0, 99)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent / 100f)
                            }
                        }
                    }
                }
            }

            if (total > 0L && downloaded < total) {
                throw UpdateException("下载不完整，请重试。")
            }

            val actualSha = digest.digest().joinToString("") { byte ->
                String.format(Locale.US, "%02x", byte)
            }
            val expectedSha = info.sha256?.trim()?.lowercase(Locale.US).orEmpty()
            if (expectedSha.isNotBlank() && expectedSha != actualSha) {
                throw UpdateException("安装包校验失败（SHA-256 不匹配），已删除下载文件。")
            }

            if (!part.renameTo(target)) {
                part.copyTo(target, overwrite = true)
                part.delete()
            }
            onProgress(1f)
            target
        } finally {
            connection.disconnect()
            if (part.exists()) part.delete()
        }
    }

    // ------------------------------------------------------------------ feed

    private fun fetchFromFeed(): UpdateInfo? {
        val connection = openConnection(UpdateConfig.FEED_URL, "application/json")
        return try {
            if (connection.responseCode !in 200..299) return null
            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(text)
            val versionName = json.optString("versionName")
                .ifBlank { json.optString("tag").removePrefix("v") }
                .ifBlank { return null }
            val tag = json.optString("tag").ifBlank { "v$versionName" }
            val assetName = json.optString("apkAsset").ifBlank { "Inter-$versionName.apk" }
            val apkUrl = json.optString("apkUrl").ifBlank {
                "https://github.com/${UpdateConfig.OWNER}/${UpdateConfig.REPO}/releases/download/$tag/$assetName"
            }
            UpdateInfo(
                versionName = versionName.removePrefix("v"),
                versionCode = json.optInt("versionCode", 0),
                notes = json.optString("notes"),
                apkUrl = apkUrl,
                assetName = assetName,
                pageUrl = json.optString("pageUrl")
                    .ifBlank { "${UpdateConfig.RELEASES_PAGE}/tag/$tag" },
                publishedAt = json.optString("publishedAt"),
                sha256 = json.optString("sha256").ifBlank { null },
                sizeBytes = json.optLong("sizeBytes", 0L),
            )
        } finally {
            connection.disconnect()
        }
    }

    // ------------------------------------------------------------------ GitHub API

    private fun fetchFromApi(): UpdateInfo {
        val connection = openConnection(UpdateConfig.LATEST_RELEASE_API, "application/vnd.github+json")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        return try {
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                throw UpdateException(apiErrorMessage(statusCode))
            }
            val text = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(text)
            val tag = json.optString("tag_name").ifBlank {
                throw UpdateException("最新版本信息不完整，请到发布页查看。")
            }
            val versionName = tag.removePrefix("v")

            var apkName = ""
            var apkUrl = ""
            var size = 0L
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkName = name
                        apkUrl = asset.optString("browser_download_url")
                        size = asset.optLong("size", 0L)
                        break
                    }
                }
            }
            if (apkUrl.isBlank()) {
                throw UpdateException("最新版本没有附带 APK，请到发布页手动下载。")
            }

            UpdateInfo(
                versionName = versionName,
                versionCode = 0,
                notes = json.optString("body"),
                apkUrl = apkUrl,
                assetName = apkName.ifBlank { "Inter-$versionName.apk" },
                pageUrl = json.optString("html_url").ifBlank { "${UpdateConfig.RELEASES_PAGE}/tag/$tag" },
                publishedAt = json.optString("published_at"),
                sha256 = null,
                sizeBytes = size,
            )
        } finally {
            connection.disconnect()
        }
    }

    // ------------------------------------------------------------------ helpers

    private fun openConnection(url: String, accept: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", accept)
            setRequestProperty("User-Agent", UpdateConfig.USER_AGENT)
        }
    }

    private fun apiErrorMessage(statusCode: Int): String = when (statusCode) {
        403, 429 -> "GitHub 访问太频繁，请过一会儿再试。"
        404 -> "还没有找到已发布的版本（GitHub 仓库还没有 Release）。"
        in 500..599 -> "GitHub 暂时不可用，请稍后再试（HTTP $statusCode）。"
        else -> "检查更新失败（HTTP $statusCode）。"
    }
}
