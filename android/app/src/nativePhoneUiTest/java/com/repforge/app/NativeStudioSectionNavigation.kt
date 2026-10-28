package com.repforge.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule

/** Navigate the real collapsed Studio settings cards before exercising their native controls. */
internal fun ComposeTestRule.revealSettingsControl(tag: String, section: String, marker: String = tag) {
    onNodeWithTag("settings_list").performScrollToNode(hasTestTag(section))
    onNodeWithTag(section).performScrollTo().assertIsDisplayed()
    val shouldExpand = if (section == "settings_theme_summary" || section == "settings_profile_summary") {
        // Detail items may be disposed by LazyColumn after a language change.
        // Read the header's actual expansion indicator instead of inferring state
        // from an offscreen child. The indicator exists in both states.
        val indicator = onNode(
            (hasText("⌄") or hasText("›")) and hasAnyAncestor(hasTestTag(section)),
            useUnmergedTree = true
        )
        indicator.assertExists()
        indicator.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text]
            .single().text == "›"
    } else {
        // The backup case is first opened here; its summary has no disclosure glyph.
        onAllNodesWithTag(marker).fetchSemanticsNodes().isEmpty()
    }
    if (shouldExpand) onNodeWithTag(section).performClick()
    onNodeWithTag("settings_list").performScrollToNode(hasTestTag(tag))
    onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
}
