package com.inter.calc.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

sealed interface InstallResult {
    data object Started : InstallResult
    data object NeedPermission : InstallResult
    data class Failed(val message: String) : InstallResult
}

object UpdateInstaller {

    fun canInstallPackages(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openUnknownSourcesSettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun install(context: Context, apkFile: File): InstallResult {
        if (!canInstallPackages(context)) {
            openUnknownSourcesSettings(context)
            return InstallResult.NeedPermission
        }
        return try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            InstallResult.Started
        } catch (e: Exception) {
            InstallResult.Failed("无法打开系统安装程序：${e.message ?: "未知错误"}")
        }
    }

    /** 仅用于首次迁移：v1.2.0 与 v1.2.1 签名不同，必须先卸载。 */
    fun openUninstall(context: Context): Boolean {
        return runCatching {
            context.startActivity(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        }.getOrDefault(false)
    }

    fun openReleasePage(context: Context, url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
