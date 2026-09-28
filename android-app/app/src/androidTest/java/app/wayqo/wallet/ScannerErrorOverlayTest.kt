// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ScannerErrorOverlayTest {
    @get:Rule val compose = createComposeRule()

    @Test fun errorOverlaysPreviewAndResumesAutomaticallyAfterThreeSeconds() {
        var resumes = 0
        val error = mutableStateOf("This is a Wi-Fi setup QR, rather than a payment QR.")
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Box {
                    Text("Camera preview remains mounted")
                    if (error.value.isNotBlank()) ScannerErrorOverlay(error.value, onResume = {
                        resumes++
                        error.value = ""
                    })
                }
            }
        }
        compose.onNodeWithText("This QR can’t be used for payment").assertIsDisplayed()
        compose.onNodeWithText("Camera preview remains mounted").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(2_500)
        compose.onNodeWithText("This QR can’t be used for payment").assertIsDisplayed()
        assertEquals(0, resumes)
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithText("This QR can’t be used for payment").assertDoesNotExist()
        compose.onNodeWithText("Camera preview remains mounted").assertIsDisplayed()
        assertEquals(1, resumes)
        compose.mainClock.advanceTimeBy(5_000)
        assertEquals(1, resumes)
    }
}
