package com.inter.calc.update

import android.content.Context

class UpdateSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("inter_update", Context.MODE_PRIVATE)

    fun lastCheckAt(): Long = prefs.getLong(KEY_LAST_CHECK, 0L)

    fun markChecked(now: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
    }

    fun lastAutoDownloadVersion(): String = prefs.getString(KEY_AUTO_DOWNLOAD_VERSION, "") ?: ""

    fun markAutoDownloadVersion(version: String) {
        prefs.edit().putString(KEY_AUTO_DOWNLOAD_VERSION, version).apply()
    }

    fun isBannerDismissed(version: String): Boolean =
        prefs.getString(KEY_DISMISSED_VERSION, "") == version

    fun dismissBanner(version: String) {
        prefs.edit().putString(KEY_DISMISSED_VERSION, version).apply()
    }

    companion object {
        private const val KEY_LAST_CHECK = "last_check_at"
        private const val KEY_AUTO_DOWNLOAD_VERSION = "auto_download_version"
        private const val KEY_DISMISSED_VERSION = "dismissed_version"
    }
}
