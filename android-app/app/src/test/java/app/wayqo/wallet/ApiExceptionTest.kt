// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiExceptionTest {
    @Test
    fun onlyStructuredBackendCodeRoutesToKyc() {
        assertTrue(ApiException(code = "kyc_required", status = 403, message = "verify").requiresKyc)
        assertFalse(ApiException(code = "provider_error", status = 502, message = "failed").requiresKyc)
        assertFalse(ApiException(status = 403, message = "KYC required in free text").requiresKyc)
    }
}
