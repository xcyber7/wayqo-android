// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Fixed pixelated redaction. Its pixels never depend on the balance or its length. */
@Composable
internal fun HiddenBalanceMosaic() {
    Canvas(Modifier.width(188.dp).height(46.dp)) {
        val columns = 28
        val rows = 7
        val cellWidth = size.width / columns
        val cellHeight = size.height / rows
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val noise = (column * 37 + row * 19 + column * row * 7) % 17
                val middle = row in 1..5 && column in 2..25
                val dark = middle && noise < 11 &&
                    !(column % 7 == 0 && row in 2..4)
                val alpha = when {
                    dark -> 0.56f + (noise % 4) * 0.07f
                    noise < 5 -> 0.16f
                    else -> 0.06f
                }
                drawRect(
                    color = Color(0xFF24232A).copy(alpha = alpha),
                    topLeft = Offset(column * cellWidth, row * cellHeight),
                    size = Size(cellWidth, cellHeight),
                )
            }
        }
    }
}
