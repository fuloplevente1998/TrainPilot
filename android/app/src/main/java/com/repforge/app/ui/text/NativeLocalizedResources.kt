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
