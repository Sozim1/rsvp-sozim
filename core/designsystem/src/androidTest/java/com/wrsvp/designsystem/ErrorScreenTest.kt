package com.wrsvp.designsystem

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class ErrorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun errorScreenShowsActions() {
        composeRule.setContent {
            ErrorScreen(
                title = "Erro",
                message = "Nao foi possivel carregar",
                primaryActionLabel = "Voltar",
                onPrimaryAction = {},
                secondaryActionLabel = "Tentar",
                onSecondaryAction = {},
            )
        }

        composeRule.onNodeWithText("Erro").assertIsDisplayed()
        composeRule.onNodeWithText("Voltar").assertIsDisplayed()
        composeRule.onNodeWithText("Tentar").assertIsDisplayed()
    }
}
