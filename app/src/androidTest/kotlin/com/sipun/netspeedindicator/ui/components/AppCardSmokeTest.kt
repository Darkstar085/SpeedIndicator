package com.sipun.netspeedindicator.ui.components

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Minimal smoke coverage for the shared UI components. Screens are backed by
 * Hilt ViewModels and are covered by their unit tests; this verifies the
 * reusable building blocks actually compose and render on device.
 */
@RunWith(AndroidJUnit4::class)
class AppCardSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appCardRendersItsContent() {
        composeRule.setContent {
            AppCard {
                Text("Net Speed Indicator")
            }
        }

        composeRule.onNodeWithText("Net Speed Indicator").assertIsDisplayed()
    }

    @Test
    fun appCardRendersMultipleLines() {
        composeRule.setContent {
            AppCard {
                Text("Download")
                Text("1.5 MB/s")
            }
        }

        composeRule.onNodeWithText("Download").assertIsDisplayed()
        composeRule.onNodeWithText("1.5 MB/s").assertIsDisplayed()
    }
}
