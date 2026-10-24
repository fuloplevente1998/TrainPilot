package com.repforge.app.ui.health

import com.repforge.app.HealthJournalStore
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth

internal data class NativeHealthQuery(val day: LocalDate = LocalDate.now(),
    val month: YearMonth = YearMonth.from(day), val channel: String = "journal") {
    init {
        require(YearMonth.from(day) == month)
        require(channel in setOf("journal", "health_connect", "samsung_health", "legacy", "ble"))
    }
    fun shift(delta: Long, today: LocalDate = LocalDate.now()): NativeHealthQuery {
        val next = month.plusMonths(delta)
        require(next <= YearMonth.from(today))
        return copy(month = next, day = if (next == YearMonth.from(today)) today else next.atEndOfMonth())
    }
}

internal class NativeHealthHistory(private val store: HealthJournalStore) {
    data class Page(val query: NativeHealthQuery, val days: List<String>, val selected: JSONObject)
    fun load(query: NativeHealthQuery): Page {
        val response = store.page(query.month.atDay(1).toString(), query.month.atEndOfMonth().toString(), query.channel, "", 0, 0, 1)
        val array = response.optJSONArray("days")
        val days = (0 until (array?.length() ?: 0)).mapNotNull { array?.optJSONObject(it) }
        // No fallback from a missing selected day to an earlier reading presented as today's value.
        val selected = days.firstOrNull { it.optString("day") == query.day.toString() }
            ?: JSONObject().put("day", query.day.toString())
        return Page(query, days.map { it.getString("day") }.distinct(), JSONObject(selected.toString()))
    }
}
