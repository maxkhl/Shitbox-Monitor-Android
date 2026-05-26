package com.shitbox.monitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import com.shitbox.monitor.data.ApiClient
import com.shitbox.monitor.data.DashboardSnapshot
import com.shitbox.monitor.data.ServerSettings
import com.shitbox.monitor.data.SettingsStore
import com.shitbox.monitor.ui.theme.Accent
import com.shitbox.monitor.ui.theme.Bg
import com.shitbox.monitor.ui.theme.Muted
import com.shitbox.monitor.ui.theme.TextMain
import com.shitbox.monitor.ui.theme.Warn
import com.shitbox.monitor.widget.MonitorWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val POLL_MS = 5000L

@Composable
fun Dashboard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(SettingsStore.load(context)) }
    var snapshot by remember { mutableStateOf<DashboardSnapshot?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(settings) {
        snapshot = null
        error = null
        while (true) {
            try {
                snapshot = DashboardSnapshot.from(ApiClient.fetchSnapshot(settings))
                error = null
            } catch (t: Throwable) {
                error = t.message
            }
            delay(POLL_MS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header(
            live = snapshot != null && error == null,
            error = error,
            onSettings = { showSettings = true },
        )
        val s = snapshot
        if (s == null && error == null) {
            Placeholder("WARTE AUF QUELLEN")
        } else {
            s?.gps?.let     { GpsCard(it) }
            s?.battery?.let { BatteryCard(it) }
            s?.solar?.let   { SolarCard(it) }
            s?.mobile?.let  { MobileCard(it) }
            s?.others?.forEach { GenericCard(it) }
        }
    }

    if (showSettings) {
        SettingsDialog(
            current = settings,
            onDismiss = { showSettings = false },
            onSave = { newSettings ->
                SettingsStore.save(context, newSettings)
                settings = SettingsStore.load(context)
                scope.launch {
                    withContext(Dispatchers.IO) {
                        runCatching { MonitorWidget().updateAll(context) }
                    }
                }
            },
        )
    }
}

@Composable
private fun Header(live: Boolean, error: String?, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "SHITBOX // MONITOR",
            color = Accent,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            letterSpacing = 2.sp,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (live) Accent else if (error != null) Warn else Muted)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = error?.let { "FEHLER" } ?: if (live) "LIVE" else "WARTE...",
                color = if (error != null) Warn else Muted,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "⚙",
                color = Muted,
                fontSize = 20.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onSettings)
                    .padding(4.dp),
            )
        }
    }
}

@Composable
private fun Placeholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Muted, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun GenericCard(entry: com.shitbox.monitor.data.SourceEntry) {
    Card(accent = TextMain) {
        Text(entry.name, color = Muted, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
    }
}
