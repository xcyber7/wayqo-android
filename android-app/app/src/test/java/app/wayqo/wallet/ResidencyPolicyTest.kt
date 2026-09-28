// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResidencyPolicyTest {
    @Test
    fun `launch exclusions match backend policy`() {
        assertTrue("US must be excluded", "US" in EXCLUDED_RESIDENCIES)
        assertTrue("Hong Kong must be excluded", "HK" in EXCLUDED_RESIDENCIES)
        assertTrue("current high-risk jurisdictions must remain excluded",
            setOf("CU", "IR", "KP", "SY").all { it in EXCLUDED_RESIDENCIES })
        assertFalse("Singapore remains eligible", "SG" in EXCLUDED_RESIDENCIES)
    }
}
