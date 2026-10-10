package tribixbite.cleverkeys

import org.junit.Assert.*
import org.junit.Test
import tribixbite.cleverkeys.backup.PrefValue
import tribixbite.cleverkeys.backup.SETTINGS_DEFAULTS
import tribixbite.cleverkeys.backup.SettingsValidation

/** Contract tests for travel thresholds, a usable pause band and portable options. */
class EditingSettingsPolicyTest {
    @Test fun exportedDefaultsHaveTheRequestedTypesAndTheSafeTapMode() {
        assertEquals(PrefValue.IntV(0), SETTINGS_DEFAULTS["backspace_tap_mode"])
        assertEquals(PrefValue.Bool(true), SETTINGS_DEFAULTS["backspace_release_delete"])
        assertEquals(PrefValue.Str(Defaults.SLIDER_SENSITIVITY), SETTINGS_DEFAULTS["slider_sensitivity"])
        assertEquals(PrefValue.FloatV(Defaults.SLIDER_SPEED_SMOOTHING), SETTINGS_DEFAULTS["slider_speed_smoothing"])
        assertEquals(PrefValue.FloatV(Defaults.SLIDER_SPEED_MAX), SETTINGS_DEFAULTS["slider_speed_max"])
        assertEquals(PrefValue.Bool(false), SETTINGS_DEFAULTS["format_search_fields"])
        assertNull(SETTINGS_DEFAULTS["backspace_undo_swipe"])
        assertNull(SETTINGS_DEFAULTS["backspace_undo_autocorrect"])
        assertTrue("backspace_undo_swipe" in SettingsValidation.DEPRECATED_KEYS)
        assertTrue("backspace_undo_autocorrect" in SettingsValidation.DEPRECATED_KEYS)
    }

    @Test fun backupRangesAcceptBothEndsAndRejectOutOfRangeValues() {
        val ranges = mapOf(
            "backspace_tap_mode" to 0..2)
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
    @Test fun obsoleteRepeatAndBrakeKeysAreExcludedFromBackupDefaults() {
        for (key in listOf("backspace_pause_enabled", "backspace_pause_dp", "backspace_resume_dp",
            "backspace_speed_percent", "backspace_fast_percent", "backspace_accel_percent")) {
            assertNull(SETTINGS_DEFAULTS[key])
            assertTrue(key in SettingsValidation.DEPRECATED_KEYS)
        }
    }
}
