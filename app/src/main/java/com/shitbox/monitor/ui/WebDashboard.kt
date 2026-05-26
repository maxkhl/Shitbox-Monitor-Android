package com.shitbox.monitor.ui

import android.annotation.SuppressLint
import android.webkit.HttpAuthHandler
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
import com.shitbox.monitor.data.SettingsStore
import com.shitbox.monitor.ui.theme.Bg
import com.shitbox.monitor.ui.theme.Muted
import com.shitbox.monitor.ui.theme.Surface
import com.shitbox.monitor.widget.MonitorWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                        runCatching { MonitorWidget().updateAll(context) }
                    }
                }
            },
        )
    }
}
