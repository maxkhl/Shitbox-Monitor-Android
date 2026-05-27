package com.shitbox.monitor.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.shitbox.monitor.MainActivity
import com.shitbox.monitor.R
import com.shitbox.monitor.data.ApiClient
import com.shitbox.monitor.data.DashboardSnapshot
import com.shitbox.monitor.data.SettingsStore
import com.shitbox.monitor.data.signalBars

object MonitorWidget {

    suspend fun updateAll(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val ids = mgr.getAppWidgetIds(ComponentName(context, MonitorWidgetReceiver::class.java))
        if (ids.isEmpty()) return

        val settings = SettingsStore.load(context)
        val snapshot = try {
            DashboardSnapshot.from(ApiClient.fetchSnapshot(settings))
        } catch (t: Throwable) {
            null
        }
        val views = buildViews(context, snapshot)
        for (id in ids) {
            mgr.updateAppWidget(id, views)
        }
    }

    private fun buildViews(context: Context, s: DashboardSnapshot?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.monitor_widget)

        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        views.setOnClickPendingIntent(R.id.widget_root, openIntent)

        if (s == null) {
            views.setViewVisibility(R.id.offline_text, View.VISIBLE)
            views.setViewVisibility(R.id.content_row, View.GONE)
            return views
        }

        views.setViewVisibility(R.id.offline_text, View.GONE)
        views.setViewVisibility(R.id.content_row, View.VISIBLE)

        // BATT
        val soc = s.battery?.soc
        if (soc != null) {
            val pct = soc.coerceIn(0.0, 100.0)
            views.setTextViewText(R.id.batt_value, "${pct.toInt()}%")
            val color = when {
                pct < 20 -> ContextCompat.getColor(context, R.color.warn)
                pct < 50 -> ContextCompat.getColor(context, R.color.accent2)
                else     -> ContextCompat.getColor(context, R.color.accent)
            }
            views.setTextColor(R.id.batt_value, color)
        } else {
            views.setTextViewText(R.id.batt_value, "—")
            views.setTextColor(R.id.batt_value, ContextCompat.getColor(context, R.color.widget_label))
        }

        // SOLAR
        val watts = s.solar?.solarPower
        views.setTextViewText(R.id.solar_value, watts?.toInt()?.let { "$it W" } ?: "—")

        // SIGNAL
        val dbm = s.mobile?.signalDbm
        views.setTextViewText(R.id.signal_value, dbm?.toInt()?.let { "$it dBm" } ?: "—")
        val bars = signalBars(dbm)
        val activeColor = ContextCompat.getColor(context, R.color.accent3)
        val inactiveColor = ContextCompat.getColor(context, R.color.border)
        val barIds = intArrayOf(R.id.bar1, R.id.bar2, R.id.bar3, R.id.bar4, R.id.bar5)
        for ((i, id) in barIds.withIndex()) {
            val on = (i + 1) <= bars
            views.setInt(id, "setBackgroundColor", if (on) activeColor else inactiveColor)
        }

        return views
    }
}
