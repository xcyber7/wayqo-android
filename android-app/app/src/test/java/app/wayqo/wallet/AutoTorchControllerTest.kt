// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoTorchControllerTest {
    @Test
    fun sustainedDarknessEnablesTorchExactlyOnce() {
        var now = 0L
        val controller = AutoTorchController(darkThreshold = 50, requiredDarkFrames = 3, exposureWarmupMs = 0, requiredDarkMs = 200, clockMs = { now })

        assertNull(controller.observe(20))
        now = 100
        assertNull(controller.observe(30))
        now = 200
        assertEquals(true, controller.observe(40))
        assertNull(controller.observe(10))
    }

    @Test
    fun aBrightFrameResetsTheLowLightWindow() {
        var now = 0L
        val controller = AutoTorchController(darkThreshold = 50, requiredDarkFrames = 3, exposureWarmupMs = 0, requiredDarkMs = 200, clockMs = { now })

        assertNull(controller.observe(20))
        now = 100
        assertNull(controller.observe(80))
        now = 200
        assertNull(controller.observe(20))
        now = 300
        assertNull(controller.observe(20))
        now = 400
        assertEquals(true, controller.observe(20))
    }

    @Test
    fun exposureWarmupAndBriefDarkFramesDoNotEnableTorch() {
        var now = 0L
        val controller = AutoTorchController(exposureWarmupMs = 2_000, requiredDarkMs = 1_200, clockMs = { now })

        repeat(8) { now += 200; assertNull(controller.observe(0)) }
        now = 2_000
        assertNull(controller.observe(20))
        now = 2_500
        assertNull(controller.observe(20))
        now = 2_600
        assertNull(controller.observe(80))
        repeat(5) { now += 200; assertNull(controller.observe(20)) }
        now += 200
        assertNull(controller.observe(20))
        now += 200
        assertEquals(true, controller.observe(20))
    }

    @Test
    fun sustainedBrightLightTurnsTorchOffWithoutFlapping() {
        var now = 0L
        val controller = AutoTorchController(darkThreshold = 50, brightThreshold = 115,
            requiredDarkFrames = 1, exposureWarmupMs = 0, requiredDarkMs = 0,
            requiredBrightMs = 300, clockMs = { now })

        assertEquals(true, controller.observe(20))
        now = 100
        assertNull(controller.observe(140))
        now = 200
        assertNull(controller.observe(100)) // brief brightness resets the off timer
        now = 300
        assertNull(controller.observe(140))
        now = 500
        assertNull(controller.observe(140))
        now = 600
        assertEquals(false, controller.observe(140))
        assertNull(controller.observe(140))
    }
}
