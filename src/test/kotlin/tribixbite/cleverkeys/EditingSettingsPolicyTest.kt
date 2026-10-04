package tribixbite.cleverkeys

import org.junit.Assert.*
import org.junit.Test
import tribixbite.cleverkeys.backup.PrefValue
import tribixbite.cleverkeys.backup.SETTINGS_DEFAULTS
import tribixbite.cleverkeys.backup.SettingsValidation

/** Contract tests for travel thresholds, a usable pause band and portable options. */
class EditingSettingsPolicyTest {
    private val options = EditBehaviorOptions()

    @Test fun halfScreenStillUsesTheSlowerRateAndBeyondUsesDoubleTheOldRate() {
        val old = BackspaceGesture.repeatDelay(500f, 1000f, -1)
        assertEquals((old * 100f / 80).toLong(),
            BackspaceGesture.repeatDelay(500f, 1000f, -1, 500f, 1000f, options))
        assertEquals((old / 2f).toLong(),
            BackspaceGesture.repeatDelay(500f, 1000f, -1, 501f, 1000f, options))
        // Near the edge, doubling must not be defeated by the previous 30 ms floor.
        assertEquals(15L, BackspaceGesture.repeatDelay(0f, 1000f, -1, 800f, 1000f, options))
    }

    @Test fun accelerationUsesTravelRatherThanBeingNearTheScreenEdge() {
        val normal = BackspaceGesture.repeatDelay(0f, 1000f, -1, 30f, 1000f, options)
        assertEquals(37L, normal)
        assertEquals(normal, BackspaceGesture.repeatDelay(0f, 1000f, -1, 800f, 0f, options))
    }

    @Test fun speedAndThresholdAreConfigurable() {
        val tuned = options.copy(speedPercent = 100, fastPercent = 300, accelPercent = 30)
        assertEquals(30L, BackspaceGesture.repeatDelay(0f, 1000f, -1, 300f, 1000f, tuned))
        assertEquals(10L, BackspaceGesture.repeatDelay(0f, 1000f, -1, 301f, 1000f, tuned))
    }

    @Test fun pauseHasRoomForFingerAdjustmentBeforeResuming() {
        val drag = BackspaceGesture.Drag(900f, 24f, 6f)
        assertTrue(drag.move(100f))
        assertFalse(drag.move(105f))
        assertTrue(drag.move(106f)); assertEquals(0, drag.direction)
        for (x in listOf(107f, 110f, 105f, 120f, 129f, 83f)) assertFalse(drag.move(x))
        assertTrue(drag.move(130f)); assertEquals(1, drag.direction)
    }

    @Test fun resumingTheSameDirectionRetainsTravelButReversingStartsANewDistance() {
        val drag = BackspaceGesture.Drag(900f, 24f, 6f)
        drag.move(100f); assertEquals(800f, drag.travel(100f), 0f)
        drag.move(106f); drag.move(82f)
        assertEquals(818f, drag.travel(82f), 0f)
        drag.move(88f); drag.move(112f)
        assertEquals(24f, drag.travel(112f), 0f)
    }

    @Test fun pauseCanBeDisabledForDirectReversal() {
        val drag = BackspaceGesture.Drag(900f, 24f, 6f, pauseEnabled = false)
        drag.move(100f); assertTrue(drag.move(106f)); assertEquals(1, drag.direction)
    }

    @Test fun exportedDefaultsHaveTheRequestedTypesAndTheSafeTapMode() {
        assertEquals(PrefValue.IntV(0), SETTINGS_DEFAULTS["backspace_tap_mode"])
        assertEquals(PrefValue.Bool(true), SETTINGS_DEFAULTS["backspace_release_delete"])
        assertEquals(PrefValue.IntV(80), SETTINGS_DEFAULTS["backspace_speed_percent"])
        assertEquals(PrefValue.IntV(200), SETTINGS_DEFAULTS["backspace_fast_percent"])
        assertEquals(PrefValue.Bool(false), SETTINGS_DEFAULTS["format_search_fields"])
        assertNull(SETTINGS_DEFAULTS["backspace_undo_swipe"])
        assertNull(SETTINGS_DEFAULTS["backspace_undo_autocorrect"])
        assertTrue("backspace_undo_swipe" in SettingsValidation.DEPRECATED_KEYS)
        assertTrue("backspace_undo_autocorrect" in SettingsValidation.DEPRECATED_KEYS)
    }

    @Test fun backupRangesAcceptBothEndsAndRejectOutOfRangeValues() {
        val ranges = mapOf(
            "backspace_tap_mode" to 0..2, "backspace_pause_dp" to 3..12,
            "backspace_resume_dp" to 12..48, "backspace_speed_percent" to 40..150,
            "backspace_fast_percent" to 100..300, "backspace_accel_percent" to 30..80)
        for ((key, range) in ranges) {
            assertEquals(range, SettingsValidation.intRangeFor(key))
            assertNull(SettingsValidation.validate(key, PrefValue.IntV(range.first)))
            assertNull(SettingsValidation.validate(key, PrefValue.IntV(range.last)))
            assertNotNull(SettingsValidation.validate(key, PrefValue.IntV(range.first - 1)))
            assertNotNull(SettingsValidation.validate(key, PrefValue.IntV(range.last + 1)))
        }
    }

    @Test fun punctuationControlsAreIndependent() {
        assertEquals(SmartAutoSpace.PunctuationEdit(0, ", ", true),
            SmartAutoSpace.punctuationEdit(',', "word ", "", removeSpace = false))
        assertEquals(SmartAutoSpace.PunctuationEdit(1, ",", false),
            SmartAutoSpace.punctuationEdit(',', "word ", "", followingSpace = false))
        assertEquals(SmartAutoSpace.PunctuationEdit(0, ",", false),
            SmartAutoSpace.punctuationEdit(',', "word ", "", false, false))
    }
}
