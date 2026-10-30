package com.repforge.app.data.migration

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LegacySnapshotParserTest {
    @Test fun decodesJsonStringsAndPreservesUnknownFields() {
        val text = "Árvíztűrő 🏋️ \"idézet\"\nkövetkező sor"
        val decoded = LegacyWebViewMigrator.decodeSnapshot(mapOf(
            "history" to JSONArray().put(JSONObject().put("started", "2026-10-22T10:00:00Z").put("notes", text)).toString(),
            "language" to "\"hu\"",
            "futureData" to JSONObject().put("value", text).toString()
        ))
        assertEquals(text, decoded.getJSONArray("history").getJSONObject(0).getString("notes"))
        assertEquals(text, decoded.getJSONObject("futureData").getString("value"))
        assertEquals("hu", decoded.getString("language"))
    }
    @Test fun rejectsMalformedHistoryInsteadOfTreatingItAsAnEmptyInstall() {
        for (raw in listOf("{broken", "null", "{}", "\"[]\"", "[{}]", "[null]")) {
            assertThrows("history=$raw", Exception::class.java) { LegacyWebViewMigrator.decodeSnapshot(mapOf("history" to raw)) }
        }
    }
    @Test fun rejectsWrongTypesForOtherRequiredCollections() {
        for (key in listOf("weights", "scheduled", "exercises", "programs", "phase3HistoryTombstones")) {
            assertThrows(Exception::class.java) { LegacyWebViewMigrator.decodeSnapshot(mapOf(key to "{}")) }
        }
        assertThrows(Exception::class.java) { LegacyWebViewMigrator.decodeSnapshot(mapOf("settings" to "[]")) }
    }
    @Test fun acceptsAGenuinelyEmptyInstall() {
        assertEquals(0, LegacyWebViewMigrator.decodeSnapshot(emptyMap()).length())
    }
}
