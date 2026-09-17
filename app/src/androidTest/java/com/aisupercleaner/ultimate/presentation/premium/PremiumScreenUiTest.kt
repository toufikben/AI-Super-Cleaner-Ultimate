package com.aisupercleaner.ultimate.presentation.premium

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.platform.app.InstrumentationRegistry
import com.aisupercleaner.ultimate.R
import org.junit.Rule
import org.junit.Test

class PremiumScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun premium_active_card_displays_active_subscription_message() {
        val title = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(R.string.premium_active_title)
        composeRule.setContent {
            PremiumActiveCard()
        }

        composeRule.onNodeWithText(title, useUnmergedTree = true).assertIsDisplayed()
    }
}
