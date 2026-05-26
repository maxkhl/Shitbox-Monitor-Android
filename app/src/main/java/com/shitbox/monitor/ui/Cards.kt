package com.shitbox.monitor.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shitbox.monitor.data.BatteryState
import com.shitbox.monitor.data.GpsState
import com.shitbox.monitor.data.MobileState
import com.shitbox.monitor.data.SolarState
import com.shitbox.monitor.data.signalBars
import com.shitbox.monitor.ui.theme.Accent
import com.shitbox.monitor.ui.theme.Accent2
import com.shitbox.monitor.ui.theme.Accent3
import com.shitbox.monitor.ui.theme.Border
import com.shitbox.monitor.ui.theme.Muted
import com.shitbox.monitor.ui.theme.Surface
import com.shitbox.monitor.ui.theme.TextMain
import com.shitbox.monitor.ui.theme.Warn

@Composable
fun Card(
    accent: Color,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Surface)
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(accent))
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (title != null) {
                Text(
                    title,
                    color = Muted,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    letterSpacing = 2.sp,
                )
            }
            content()
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(label, color = Muted, fontSize = 15.sp)
        Text(value, color = TextMain, fontFamily = FontFamily.Monospace, fontSize = 17.sp)
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
    }
}

private fun fmt(v: Double?, decimals: Int = 1): String =
    v?.let { "%.${decimals}f".format(it) } ?: "—"

private fun fmtUpdated(iso: String?): String =
    iso?.substringAfter('T')?.take(8) ?: "—"

@Composable
fun BatteryCard(b: BatteryState) {
    val alarmOk = b.alarm.isNullOrBlank() || b.alarm.equals("no_alarm", true) || b.alarm.equals("none", true)
    Card(accent = if (alarmOk) Accent else Warn, title = "⚡ ${b.name}") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SOCGauge(b.soc)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Metric("Spannung", fmt(b.voltage) + " V")
                Metric("Strom",    fmt(b.current) + " A")
                Metric("Verbraucht", fmt(b.consumedAh) + " Ah")
                Metric("Restlaufzeit", remainingLabel(b.remainingMins))
            }
        }
        Text("UPDATED ${fmtUpdated(b.updatedAt)}", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}

@Composable
private fun SOCGauge(soc: Double?) {
    val pct = (soc ?: 0.0).coerceIn(0.0, 100.0)
    val color = when {
        pct < 20 -> Warn
        pct < 50 -> Accent2
        else     -> Accent
    }
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${pct.toInt()}",
                color = color,
                fontFamily = FontFamily.Monospace,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Text("SOC %", color = Muted, fontSize = 10.sp)
        }
    }
}

private fun remainingLabel(mins: Double?): String {
    if (mins == null || mins < 0) return "—"
    if (mins > 60 * 24 * 7) return "> 7 Tage"
    val h = (mins / 60).toInt()
    val m = (mins % 60).toInt()
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Composable
fun SolarCard(s: SolarState) {
    val errOk = s.error.isNullOrBlank() || s.error.equals("no_error", true)
    Card(accent = if (errOk) Accent2 else Warn, title = "☀ ${s.name}") {
        if (s.state != null) Row { Badge(s.state, Accent2) }
        Text(
            text = (s.solarPower?.toInt()?.toString() ?: "—") + " W",
            color = Accent2,
            fontFamily = FontFamily.Monospace,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
        )
        Text("Solar-Leistung", color = Muted, fontSize = 14.sp)
        Metric("Batterie",     fmt(s.batteryVoltage) + " V")
        Metric("Ladestrom",    fmt(s.chargingCurrent) + " A")
        Metric("Ertrag heute", fmt(s.yieldToday, 0) + " Wh")
        Text("UPDATED ${fmtUpdated(s.updatedAt)}", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}

@Composable
fun GpsCard(g: GpsState) {
    val context = LocalContext.current
    Card(accent = Accent3, title = "⌖ ${g.name}") {
        Row {
            if (g.hasFix) Badge("FIX", Accent3) else Badge("NO FIX", Warn)
        }
        Metric("Lat",        g.lat?.let { "%.5f°".format(it) } ?: "—")
        Metric("Lon",        g.lon?.let { "%.5f°".format(it) } ?: "—")
        Metric("Höhe",       g.altitude?.let { "${it.toInt()} m" } ?: "—")
        Metric("Geschw.",    g.speed?.let { fmt(it) + " km/h" } ?: "—")
        Metric("Satelliten", g.satellites?.toInt()?.toString() ?: "—")
        if (g.hasFix && g.lat != null && g.lon != null) {
            Text(
                text = "📍 In Karte öffnen",
                color = Accent3,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable {
                        val lat = g.lat
                        val lon = g.lon
                        val geo = Uri.parse("geo:$lat,$lon?q=$lat,$lon(Shitbox)")
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, geo))
                        } catch (_: ActivityNotFoundException) {
                            val web = Uri.parse("https://www.google.com/maps?q=$lat,$lon")
                            context.startActivity(Intent(Intent.ACTION_VIEW, web))
                        }
                    },
            )
        }
        Text("UPDATED ${fmtUpdated(g.updatedAt)}", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}

@Composable
fun MobileCard(m: MobileState) {
    Card(accent = Accent3, title = "📶 ${m.name}") {
        if (!m.operator.isNullOrBlank()) {
            Text(m.operator, color = Muted, fontSize = 14.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!m.state.isNullOrBlank()) {
                val warn = Regex("(disconnect|down|fail|not)", RegexOption.IGNORE_CASE).containsMatchIn(m.state)
                Badge(m.state, if (warn) Warn else Accent)
            }
            if (!m.netType.isNullOrBlank()) Badge(m.netType, Accent3)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = (m.signalDbm?.toInt()?.toString() ?: "—") + " dBm",
                color = Accent3,
                fontFamily = FontFamily.Monospace,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(10.dp))
            SignalBars(signalBars(m.signalDbm))
        }
        Text(if (m.rsrp != null) "RSRP" else "Signal", color = Muted, fontSize = 14.sp)
        if (m.sinr != null) Metric("SINR", "${m.sinr.toInt()} dB")
        if (m.rsrq != null) Metric("RSRQ", "${m.rsrq.toInt()} dB")
        if (!m.band.isNullOrBlank()) Metric("Band", m.band)
        if (m.rsrp != null && m.rssi != null) Metric("RSSI", "${m.rssi.toInt()} dBm")
        Text("UPDATED ${fmtUpdated(m.updatedAt)}", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}

@Composable
fun SignalBars(active: Int, height: androidx.compose.ui.unit.Dp = 22.dp) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (i in 1..5) {
            val on = i <= active
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(height * (i * 0.2f))
                    .background(if (on) Accent3 else Border)
            )
        }
    }
}
