package com.repforge.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NativeTrainingPrescriptionParityTest(private val name: String, private val row: JSONObject) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}") fun cases(): List<Array<Any>> {
            val raw = requireNotNull(NativeTrainingPrescriptionParityTest::class.java
                .getResourceAsStream("/native-prescription-main-7473.json")).bufferedReader().use { it.readText() }
            val fixture = JSONObject(raw)
            require(fixture.getString("blob") == "b5567576bed9a8f93608a6bf1ed8db1dee4723f4")
            val cases = fixture.getJSONArray("cases")
            return (0 until cases.length()).map { cases.getJSONObject(it).let { item -> arrayOf<Any>(item.getString("name"), item) } }
        }
    }
    @Test fun matchesActualMainRecipeAndLevel2Overrides() {
        val lib = row.getJSONObject("library"); val profile = row.getJSONObject("profile")
        val actual = if (row.getBoolean("level2")) NativeTrainingPrescription.level2(lib, profile)
            else NativeTrainingPrescription.create(lib, profile)
        val expected = row.getJSONObject("expected")
        assertEquals(expected.keys().asSequence().toSet(), actual.keys().asSequence().toSet())
        for (key in expected.keys()) {
            val a = actual.get(key); val e = expected.get(key)
            if (a is Number && e is Number) assertEquals(key, e.toDouble(), a.toDouble(), .0001)
            else assertEquals(key, e, a)
        }
    }
}
