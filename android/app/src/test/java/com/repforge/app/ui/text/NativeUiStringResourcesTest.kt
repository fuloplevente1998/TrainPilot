package com.repforge.app.ui.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * JVM-only resource consistency test: it does not claim to exercise Android locale switching.
 * Run with :app:testDebugUnitTest when an Android SDK / Gradle runner is available.
 */
class NativeUiStringResourcesTest {
    private val folders = listOf("values", "values-en", "values-de", "values-ro")
    private val placeholders = Regex("%(?:[1-9][0-9]*\\$)?[dsf]")

    private fun readStrings(folder: String): Map<String, String> {
        val xml = File("src/main/res/$folder/strings.xml")
        assertTrue("Missing resource file: $xml", xml.isFile)
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        val nodes = factory.newDocumentBuilder().parse(xml).getElementsByTagName("string")
        val values = linkedMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val item = nodes.item(i) as Element
            val key = item.getAttribute("name")
            assertFalse("Empty string key: $folder", key.isBlank())
            assertFalse("Duplicate string key: $folder / $key", values.containsKey(key))
            values[key] = item.textContent.orEmpty()
        }
        return values
    }

    @Test
    fun allFourLocalesContainTheSameNonemptyNativeUiKeys() {
        val resources = folders.associateWith(::readStrings)
        val reference = resources.getValue("values").filterKeys { it.startsWith("native_") }
        assertTrue("Expected native UI localization keys", reference.isNotEmpty())

        for ((folder, strings) in resources) {
            val localized = strings.filterKeys { it.startsWith("native_") }
            assertEquals("Resource keys differ for $folder", reference.keys, localized.keys)
            for ((key, value) in localized) {
                assertFalse("Blank translation: $folder / $key", value.isBlank())
            }
        }
    }

    @Test
    fun nativeUiFormatParametersMatchAcrossLocales() {
        val resources = folders.associateWith(::readStrings)
        val reference = resources.getValue("values").filterKeys { it.startsWith("native_") }
        for ((key, huValue) in reference) {
            val expected = placeholders.findAll(huValue).map { it.value }.toList().sorted()
            for ((folder, strings) in resources) {
                val found = placeholders.findAll(strings.getValue(key)).map { it.value }.toList().sorted()
                assertEquals("Parameter mismatch for $folder / $key", expected, found)
            }
        }
    }

    @Test
    fun sensitiveWorkoutStatesAndUnitsArePresentInEveryLocale() {
        val required = listOf(
            "native_quick_library_error",
            "native_calendar_24",
            "native_profile_16",
            "native_settings_erase_message",
            "native_workout_gps_permission",
            "native_workout_component_distance",
            "native_workout_component_weight",
            "native_workout_component_bilateral"
        )
        for (folder in folders) {
            val strings = readStrings(folder)
            for (key in required) assertTrue("Missing $folder / $key", strings[key]?.isNotBlank() == true)
            assertTrue(strings.getValue("native_workout_component_distance").contains("(km)"))
            assertTrue(strings.getValue("native_workout_component_weight").contains("(kg)"))
        }
    }
}
