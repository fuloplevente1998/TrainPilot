package com.repforge.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule

/** Navigate the real collapsed Studio settings cards before exercising their native controls. */
internal fun ComposeTestRule.revealSettingsControl(tag: String, section: String, marker: String = tag) {
    onNodeWithTag("settings_list").performScrollToNode(hasTestTag(section))
    onNodeWithTag(section).performScrollTo().assertIsDisplayed()
    if (onAllNodesWithTag(marker).fetchSemanticsNodes().isEmpty()) {
        onNodeWithTag(section).performClick()
    }
    onNodeWithTag("settings_list").performScrollToNode(hasTestTag(tag))
    onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
}
