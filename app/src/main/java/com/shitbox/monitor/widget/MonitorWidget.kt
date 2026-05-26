package com.shitbox.monitor.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import com.shitbox.monitor.MainActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.shitbox.monitor.data.ApiClient
import com.shitbox.monitor.data.DashboardSnapshot
import com.shitbox.monitor.data.SettingsStore
import com.shitbox.monitor.data.signalBars
import androidx.compose.ui.graphics.Color as ComposeColor

private val Bg      = ComposeColor(0xFF0b0e13)
private val Surface = ComposeColor(0xFF131820)
private val Accent  = ComposeColor(0xFF00d4aa)
private val Accent2 = ComposeColor(0xFFf0a500)
private val Accent3 = ComposeColor(0xFF4ea3ff)
private val Warn    = ComposeColor(0xFFff4d4d)
private val Muted   = ComposeColor(0xFF4a5e72)
private val TextMain = ComposeColor(0xFFd8e4f0)
private val Border  = ComposeColor(0xFF1e2733)

class MonitorWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val settings = SettingsStore.load(context)
        val snapshot = try {
            DashboardSnapshot.from(ApiClient.fetchSnapshot(settings))
        } catch (t: Throwable) {
            null
        }
        provideContent { WidgetContent(snapshot) }
    }
}

@Composable
private fun WidgetContent(s: DashboardSnapshot?) {
    val context = LocalContext.current
    val openApp = Intent(context, MainActivity::class.java)
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Surface))
            .clickable(actionStartActivity(openApp))
            .padding(12.dp),
    ) {
        if (s == null) {
            Text(
                "OFFLINE",
                style = TextStyle(
                    color = ColorProvider(Warn),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                ),
            )
            return@Box
        }

        Column(modifier = GlanceModifier.fillMaxSize()) {
            Text(
                "SHITBOX",
                style = TextStyle(
                    color = ColorProvider(Accent),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                ),
            )
            Spacer(GlanceModifier.height(6.dp))

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SocBlock(s.battery?.soc)
                Spacer(GlanceModifier.width(12.dp))
                SolarBlock(s.solar?.solarPower)
                Spacer(GlanceModifier.width(12.dp))
                SignalBlock(s.mobile?.signalDbm)
            }
        }
    }
}

@Composable
private fun SocBlock(soc: Double?) {
    val pct = (soc ?: 0.0).coerceIn(0.0, 100.0)
    val color = when {
        soc == null -> Muted
        pct < 20    -> Warn
        pct < 50    -> Accent2
        else        -> Accent
    }
    Column {
        Text(
            text = if (soc == null) "—" else "${pct.toInt()}%",
            style = TextStyle(
                color = ColorProvider(color),
                fontFamily = FontFamily.Monospace,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            "BATT",
            style = TextStyle(
                color = ColorProvider(Muted),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            ),
        )
    }
}

@Composable
private fun SolarBlock(watts: Double?) {
    Column {
        Text(
            text = watts?.toInt()?.toString()?.plus(" W") ?: "—",
            style = TextStyle(
                color = ColorProvider(Accent2),
                fontFamily = FontFamily.Monospace,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            "SOLAR",
            style = TextStyle(
                color = ColorProvider(Muted),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            ),
        )
    }
}

@Composable
private fun SignalBlock(dbm: Double?) {
    val bars = signalBars(dbm)
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            for (i in 1..5) {
                val on = i <= bars
                val barColor = if (on) Accent3 else Border
                val heightDp = (4 + i * 3).dp
                Box(
                    modifier = GlanceModifier
                        .width(4.dp)
                        .height(heightDp)
                        .background(ColorProvider(barColor))
                ) {}
                if (i < 5) Spacer(GlanceModifier.width(2.dp))
            }
        }
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = dbm?.toInt()?.toString()?.plus(" dBm") ?: "—",
            style = TextStyle(
                color = ColorProvider(TextMain),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            ),
        )
        Text(
            "LTE",
            style = TextStyle(
                color = ColorProvider(Muted),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            ),
        )
    }
}
