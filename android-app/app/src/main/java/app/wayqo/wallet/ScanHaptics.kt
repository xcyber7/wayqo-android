// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/** Uses system feedback settings; never forces vibration when the user disabled it. */
internal fun View.scanFeedback(accepted: Boolean) {
    val feedback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (accepted) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
    } else {
        if (accepted) HapticFeedbackConstants.KEYBOARD_TAP else HapticFeedbackConstants.LONG_PRESS
    }
    performHapticFeedback(feedback)
}
