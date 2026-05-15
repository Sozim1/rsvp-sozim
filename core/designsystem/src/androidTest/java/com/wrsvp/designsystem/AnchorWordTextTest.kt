package com.wrsvp.designsystem

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.sp
import org.junit.Rule
import org.junit.Test

class AnchorWordTextTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun anchorWordTextShowsCompleteWord() {
        composeRule.setContent {
            AnchorWordText(
                before = "frequ",
                anchor = "e",
                after = "nte",
                fontSize = 32.sp,
            )
        }

        composeRule.onNodeWithText("frequente").assertIsDisplayed()
    }
}
