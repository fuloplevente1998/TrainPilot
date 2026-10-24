package com.repforge.app.ui.journal

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/** DataStore language is explicit; changing it does not depend on the phone system locale. */
internal fun journalResources(context: Context, language: String): Resources {
    val configuration = Configuration(context.resources.configuration)
    configuration.setLocale(Locale.forLanguageTag(language.takeIf { it in listOf("hu", "en", "de", "ro") } ?: "hu"))
    return context.createConfigurationContext(configuration).resources
}
