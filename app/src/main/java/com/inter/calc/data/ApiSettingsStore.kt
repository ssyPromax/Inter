package com.inter.calc.data

import android.content.Context

data class DeepSeekConfig(
    val apiKey: String = "",
    val baseUrl: String = DEFAULT_BASE_URL,
    val model: String = DEFAULT_MODEL,
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    companion object {
        const val DEFAULT_BASE_URL = "https://api.deepseek.com"
        const val DEFAULT_MODEL = "deepseek-chat"
        val AVAILABLE_MODELS = listOf("deepseek-chat", "deepseek-reasoner")
    }
}

class ApiSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("inter_prefs", Context.MODE_PRIVATE)

    fun loadConfig(): DeepSeekConfig = DeepSeekConfig(
        apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
        baseUrl = prefs.getString(KEY_BASE_URL, DeepSeekConfig.DEFAULT_BASE_URL)?.ifBlank { DeepSeekConfig.DEFAULT_BASE_URL } ?: DeepSeekConfig.DEFAULT_BASE_URL,
        model = prefs.getString(KEY_MODEL, DeepSeekConfig.DEFAULT_MODEL)?.ifBlank { DeepSeekConfig.DEFAULT_MODEL } ?: DeepSeekConfig.DEFAULT_MODEL,
    )

    fun saveConfig(config: DeepSeekConfig) {
        prefs.edit()
            .putString(KEY_API_KEY, config.apiKey.trim())
            .putString(KEY_BASE_URL, config.baseUrl.trim().trimEnd('/').ifBlank { DeepSeekConfig.DEFAULT_BASE_URL })
            .putString(KEY_MODEL, config.model.trim().ifBlank { DeepSeekConfig.DEFAULT_MODEL })
            .apply()
    }

    fun loadDarkTheme(): Boolean = prefs.getBoolean(KEY_DARK_THEME, false)
    fun saveDarkTheme(dark: Boolean) { prefs.edit().putBoolean(KEY_DARK_THEME, dark).apply() }

    companion object {
        private const val KEY_API_KEY = "ds_api_key"
        private const val KEY_BASE_URL = "ds_base_url"
        private const val KEY_MODEL = "ds_model"
        private const val KEY_DARK_THEME = "dark_theme"
    }
}
