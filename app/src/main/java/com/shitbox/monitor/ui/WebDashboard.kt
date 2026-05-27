package com.shitbox.monitor.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.HttpAuthHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.updateAll
import com.shitbox.monitor.data.ServerSettings
import com.shitbox.monitor.data.SettingsStore
import com.shitbox.monitor.ui.theme.Bg
import com.shitbox.monitor.ui.theme.Muted
import com.shitbox.monitor.ui.theme.Surface
import com.shitbox.monitor.widget.MonitorWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request

private val proxyClient = OkHttpClient()

/**
 * For every WebView request to the dashboard host, fetch it via OkHttp with a
 * Basic Auth header attached. Other hosts (OSM tiles, Google Fonts, unpkg)
 * pass through to the WebView's normal loader.
 */
private fun authProxy(
    server: ServerSettings,
    request: WebResourceRequest,
): WebResourceResponse? {
    if (!server.hasCredentials) return null
    if (request.method != "GET") return null
    val targetHost = Uri.parse(server.baseUrl).host ?: return null
    if (request.url.host != targetHost) return null

    return try {
        val builder = Request.Builder()
            .url(request.url.toString())
            .header("Authorization", Credentials.basic(server.username, server.password))
        request.requestHeaders.forEach { (k, v) ->
            if (!k.equals("Authorization", ignoreCase = true)) builder.header(k, v)
        }
        val response = proxyClient.newCall(builder.build()).execute()
        val contentType = response.header("Content-Type") ?: "application/octet-stream"
        val parts = contentType.split(';').map { it.trim() }
        val mime = parts[0]
        val encoding = parts.drop(1)
            .firstOrNull { it.startsWith("charset=", ignoreCase = true) }
            ?.substringAfter('=')?.trim('"', ' ')
        val headers = response.headers.associate { it.first to it.second }
        WebResourceResponse(
            mime,
            encoding,
            response.code,
            response.message.ifEmpty { "OK" },
            headers,
            response.body?.byteStream(),
        )
    } catch (_: Exception) {
        null
    }
}

@Composable
fun WebDashboard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var server by remember { mutableStateOf(SettingsStore.load(context)) }
    var showSettings by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        key(server) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    @SuppressLint("SetJavaScriptEnabled")
                    WebView(ctx).apply {
                        setBackgroundColor(0xFF0b0e13.toInt())
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest,
                            ): WebResourceResponse? = authProxy(server, request)

                            override fun onReceivedHttpAuthRequest(
                                view: WebView,
                                handler: HttpAuthHandler,
                                host: String,
                                realm: String,
                            ) {
                                if (server.hasCredentials) {
                                    handler.proceed(server.username, server.password)
                                } else {
                                    handler.cancel()
                                }
                            }
                        }
                        loadUrl(server.baseUrl)
                    }
                },
                onRelease = { it.destroy() },
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 36.dp, end = 16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Surface.copy(alpha = 0.85f))
                .clickable { showSettings = true },
            contentAlignment = Alignment.Center,
        ) {
            Text("⚙", color = Muted, fontSize = 20.sp)
        }
    }

    if (showSettings) {
        SettingsDialog(
            current = server,
            onDismiss = { showSettings = false },
            onSave = { newSettings ->
                SettingsStore.save(context, newSettings)
                server = SettingsStore.load(context)
                scope.launch {
                    withContext(Dispatchers.IO) {
                        runCatching { MonitorWidget.updateAll(context) }
                    }
                }
            },
        )
    }
}
