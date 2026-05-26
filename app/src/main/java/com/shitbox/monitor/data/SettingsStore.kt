package com.shitbox.monitor.data

import android.content.Context

const val DEFAULT_BASE_URL = "http://192.168.1.50:8000"
private const val PREFS = "monitor_settings"
private const val KEY_BASE_URL = "base_url"
private const val KEY_USERNAME = "username"
private const val KEY_PASSWORD = "password"

data class ServerSettings(
    val baseUrl: String,
    val username: String,
    val password: String,
) {
    val hasCredentials: Boolean get() = username.isNotBlank() && password.isNotEmpty()
}

object SettingsStore {
    fun load(context: Context): ServerSettings {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return ServerSettings(
            baseUrl = p.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL,
            username = p.getString(KEY_USERNAME, "") ?: "",
            password = p.getString(KEY_PASSWORD, "") ?: "",
        )
    }

    fun save(context: Context, settings: ServerSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, settings.baseUrl.trim().trimEnd('/'))
            .putString(KEY_USERNAME, settings.username.trim())
            .putString(KEY_PASSWORD, settings.password)
            .apply()
    }
}
