package com.repforge.app.ui.text

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/** Explicit DataStore language without replacing Activity contexts needed by Health/SAF. */
internal fun nativeResources(context: Context, language: String): Resources {
    val configuration = Configuration(context.resources.configuration)
    configuration.setLocale(Locale.forLanguageTag(language.takeIf { it in listOf("hu", "en", "de", "ro") } ?: "hu"))
    return context.createConfigurationContext(configuration).resources
}

/** Stored translations only; a missing translation keeps the user's original Hungarian/name field. */
internal fun nativeLocalizedName(language: String, hu: String, en: String, de: String, ro: String): String =
    when (language) { "en" -> en; "de" -> de; "ro" -> ro; else -> hu }.ifBlank { hu }
