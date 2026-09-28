// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Notices are bundled locally and remain available without a backend or wallet. */
@Composable
internal fun LegalNoticeButton() {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    var document by remember { mutableStateOf<String?>(null) }
    val text = remember(document) {
        document?.let { context.assets.open("legal/$it").bufferedReader().use { reader -> reader.readText() } }.orEmpty()
    }
    TextButton(onClick = { document = null; visible = true }) { Text("About & licenses") }
    if (visible) AlertDialog(
        onDismissRequest = { visible = false },
        title = { Text(if (document == null) BuildConfig.EDITION else "License notices") },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (document != null) Text(text, style = MaterialTheme.typography.bodySmall)
                else {
                    Text("Version ${BuildConfig.VERSION_NAME}\nCopyright © 2026 Black Flag International Limited")
                    Text(if (BuildConfig.OPEN_EDITION)
                        "WAYQO Open is licensed under GNU GPL version 3. You may modify and redistribute it under that license. It comes without warranty, except where required by law."
                    else "WAYQO Standard is distributed under the official binary license. Third-party components retain their own licenses.")
                    TextButton(onClick = { document = if (BuildConfig.OPEN_EDITION) "GPL-3.0.txt" else "STANDARD.txt" }) { Text("Edition license") }
                    TextButton(onClick = { document = "THIRD-PARTY.txt" }) { Text("Third-party licenses") }
                    TextButton(onClick = { document = "TRADEMARKS.txt" }) { Text("Brand and trademark policy") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (document == null) visible = false else document = null }) {
            Text(if (document == null) "Close" else "Back")
        } },
    )
}
