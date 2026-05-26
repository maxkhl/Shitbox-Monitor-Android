package com.shitbox.monitor.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ApiClient {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun fetchSnapshot(settings: ServerSettings): Snapshot = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url("${settings.baseUrl}/api/data")
        if (settings.hasCredentials) {
            builder.header("Authorization", Credentials.basic(settings.username, settings.password))
        }
        http.newCall(builder.build()).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("HTTP ${resp.code}: $body")
            json.decodeFromString(body)
        }
    }
}
