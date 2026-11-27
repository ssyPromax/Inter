package com.inter.calc.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.io.File
import java.security.MessageDigest

data class ApkCheckResult(
    val ok: Boolean,
    val message: String? = null,
    val signatureMismatch: Boolean = false,
)

/**
 * 下载完成后先做本机校验，再交给系统安装：
 * 包名、版本号、签名必须都对，避免把错包或别人签名的包装上去。
 */
object ApkVerifier {

    @Suppress("DEPRECATION")
    fun verify(context: Context, apkFile: File, currentVersionCode: Int): ApkCheckResult {
        if (!apkFile.isFile || apkFile.length() == 0L) {
            return ApkCheckResult(false, "下载的安装包不存在或为空。")
        }
        val packageManager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }

        val archive = packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: return ApkCheckResult(false, "无法解析下载的安装包。")

        if (archive.packageName != context.packageName) {
            return ApkCheckResult(false, "安装包包名不匹配，已拒绝安装。")
        }

        val archiveVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archive.longVersionCode
        } else {
            archive.versionCode.toLong()
        }
        if (archiveVersionCode <= currentVersionCode.toLong()) {
            return ApkCheckResult(false, "下载的安装包版本没有高于当前版本。")
        }

        val installed = runCatching {
            packageManager.getPackageInfo(context.packageName, flags)
        }.getOrNull() ?: return ApkCheckResult(false, "无法读取当前应用的签名。")

        val archiveSignatures = certificateDigests(archive)
        val installedSignatures = certificateDigests(installed)
        if (archiveSignatures.isEmpty() || installedSignatures.isEmpty()) {
            return ApkCheckResult(false, "无法读取安装包签名，已拒绝安装。")
        }
        if (archiveSignatures != installedSignatures) {
            return ApkCheckResult(
                ok = false,
                message = "签名不一致：这个更新包不是用当前应用的签名生成的，系统会拒绝覆盖安装。",
                signatureMismatch = true,
            )
        }
        return ApkCheckResult(true)
    }

    @Suppress("DEPRECATION")
    private fun certificateDigests(info: PackageInfo): List<String> {
        val signatures: Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners ?: emptyArray()
        } else {
            info.signatures ?: emptyArray()
        }
        return signatures
            .map { signature ->
                MessageDigest.getInstance("SHA-256")
                    .digest(signature.toByteArray())
                    .joinToString("") { "%02x".format(it) }
            }
            .sorted()
    }
}
