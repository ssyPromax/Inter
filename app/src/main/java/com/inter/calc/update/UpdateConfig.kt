package com.inter.calc.update

object UpdateConfig {
    const val OWNER = "ssyPromax"
    const val REPO = "Inter"

    const val LATEST_RELEASE_API = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"
    const val FEED_URL = "https://github.com/$OWNER/$REPO/releases/latest/download/update.json"
    const val RELEASES_PAGE = "https://github.com/$OWNER/$REPO/releases"

    const val USER_AGENT = "Inter-Android-Updater"
    const val AUTO_CHECK_INTERVAL_MS = 12L * 60L * 60L * 1000L
}
