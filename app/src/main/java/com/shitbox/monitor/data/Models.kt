package com.shitbox.monitor.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class SourceEntry(
    val name: String,
    val type: String,
    val data: JsonObject,
    val updated_at: String? = null,
)

typealias Snapshot = Map<String, SourceEntry>

fun JsonObject.numberOrNull(key: String): Double? {
    val v = this[key] ?: return null
    return v.asNumberOrNull()
}

fun JsonObject.stringOrNull(key: String): String? {
    val v = this[key] ?: return null
    return (v as? JsonPrimitive)?.contentOrNull
}

private fun JsonElement.asNumberOrNull(): Double? {
    val p = this as? JsonPrimitive ?: return null
    return p.doubleOrNull ?: p.contentOrNull?.toDoubleOrNull()
}

data class DashboardSnapshot(
    val battery: BatteryState? = null,
    val solar: SolarState? = null,
    val gps: GpsState? = null,
    val mobile: MobileState? = null,
    val others: List<SourceEntry> = emptyList(),
) {
    companion object {
        fun from(raw: Snapshot): DashboardSnapshot {
            var battery: BatteryState? = null
            var solar: SolarState? = null
            var gps: GpsState? = null
            var mobile: MobileState? = null
            val others = mutableListOf<SourceEntry>()
            raw.values.forEach { entry ->
                val t = entry.type.lowercase()
                when {
                    "battery" in t || "shunt" in t -> battery = BatteryState.from(entry)
                    "solar" in t || "mppt" in t   -> solar = SolarState.from(entry)
                    "gps" in t                     -> gps = GpsState.from(entry)
                    "mobile" in t || "lte" in t   -> mobile = MobileState.from(entry)
                    else -> others += entry
                }
            }
            return DashboardSnapshot(battery, solar, gps, mobile, others)
        }
    }
}

data class BatteryState(
    val name: String,
    val soc: Double?,
    val voltage: Double?,
    val current: Double?,
    val consumedAh: Double?,
    val remainingMins: Double?,
    val alarm: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(e: SourceEntry) = BatteryState(
            name = e.name,
            soc = e.data.numberOrNull("soc"),
            voltage = e.data.numberOrNull("voltage"),
            current = e.data.numberOrNull("current"),
            consumedAh = e.data.numberOrNull("consumed_ah"),
            remainingMins = e.data.numberOrNull("remaining_mins"),
            alarm = e.data.stringOrNull("alarm"),
            updatedAt = e.updated_at,
        )
    }
}

data class SolarState(
    val name: String,
    val solarPower: Double?,
    val batteryVoltage: Double?,
    val chargingCurrent: Double?,
    val yieldToday: Double?,
    val state: String?,
    val error: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(e: SourceEntry) = SolarState(
            name = e.name,
            solarPower = e.data.numberOrNull("solar_power"),
            batteryVoltage = e.data.numberOrNull("battery_voltage"),
            chargingCurrent = e.data.numberOrNull("battery_charging_current"),
            yieldToday = e.data.numberOrNull("yield_today"),
            state = e.data.stringOrNull("charge_state"),
            error = e.data.stringOrNull("charger_error"),
            updatedAt = e.updated_at,
        )
    }
}

data class GpsState(
    val name: String,
    val lat: Double?,
    val lon: Double?,
    val altitude: Double?,
    val speed: Double?,
    val satellites: Double?,
    val hasFix: Boolean,
    val updatedAt: String?,
) {
    companion object {
        fun from(e: SourceEntry): GpsState {
            val lat = e.data.numberOrNull("latitude") ?: e.data.numberOrNull("lat")
            val lon = e.data.numberOrNull("longitude") ?: e.data.numberOrNull("lon")
            val fixCode = e.data.numberOrNull("fix_status") ?: e.data.numberOrNull("fix")
            val hasFix = if (fixCode != null) fixCode > 0
            else lat != null && lon != null && !(lat == 0.0 && lon == 0.0)
            return GpsState(
                name = e.name,
                lat = lat,
                lon = lon,
                altitude = e.data.numberOrNull("altitude"),
                speed = e.data.numberOrNull("speed"),
                satellites = e.data.numberOrNull("satellites"),
                hasFix = hasFix,
                updatedAt = e.updated_at,
            )
        }
    }
}

data class MobileState(
    val name: String,
    val operator: String?,
    val netType: String?,
    val state: String?,
    val rsrp: Double?,
    val rssi: Double?,
    val sinr: Double?,
    val rsrq: Double?,
    val band: String?,
    val updatedAt: String?,
) {
    val signalDbm: Double? get() = rsrp ?: rssi
    companion object {
        fun from(e: SourceEntry) = MobileState(
            name = e.name,
            operator = e.data.stringOrNull("operator"),
            netType = e.data.stringOrNull("conntype")
                ?: e.data.stringOrNull("conn_type")
                ?: e.data.stringOrNull("ntype"),
            state = e.data.stringOrNull("data_conn_state")
                ?: e.data.stringOrNull("conn_state")
                ?: e.data.stringOrNull("state"),
            rsrp = e.data.numberOrNull("rsrp"),
            rssi = e.data.numberOrNull("signal") ?: e.data.numberOrNull("rssi"),
            sinr = e.data.numberOrNull("sinr"),
            rsrq = e.data.numberOrNull("rsrq"),
            band = e.data.stringOrNull("band"),
            updatedAt = e.updated_at,
        )
    }
}

fun signalBars(dbm: Double?): Int {
    if (dbm == null) return 0
    return when {
        dbm >= -85  -> 5
        dbm >= -95  -> 4
        dbm >= -105 -> 3
        dbm >= -115 -> 2
        else        -> 1
    }
}
