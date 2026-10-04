package tribixbite.cleverkeys

import android.content.SharedPreferences
import io.mockk.*
import org.junit.Assert.*
import org.junit.Test

class EditingSettingsReadTest {
    private fun prefs(values: Map<String, Any> = emptyMap()): SharedPreferences {
        val prefs = mockk<SharedPreferences>()
        every { prefs.getInt(any(), any()) } answers { (values[firstArg<String>()] as? Int) ?: secondArg() }
        every { prefs.getBoolean(any(), any()) } answers { (values[firstArg<String>()] as? Boolean) ?: secondArg() }
        return prefs
    }

    @Test fun missingPreferencesMatchProductDefaults() {
        val options = readEditBehaviorPreferences(prefs())
        assertEquals(EditBehaviorOptions(), options)
        assertEquals(0, options.tapMode)
        assertFalse(options.formatSearchFields)
    }

    @Test fun legacyUndoBooleansDoNotEnableDestructiveTap() {
        assertEquals(0, readEditBehaviorPreferences(prefs(mapOf(
            "backspace_undo_swipe" to true, "backspace_undo_autocorrect" to true))).tapMode)
    }

    @Test fun malformedNumericRangesAreClampedAtRuntime() {
        val options = readEditBehaviorPreferences(prefs(mapOf(
            "backspace_tap_mode" to 99, "backspace_pause_dp" to -1,
            "backspace_resume_dp" to 999, "backspace_speed_percent" to 0,
            "backspace_fast_percent" to 999, "backspace_accel_percent" to -1)))
        assertEquals(EditBehaviorOptions(), options) // Old drag tuning is ignored.
    }

    @Test fun changedPreferencesProduceANewImmutableReadModel() {
        val values = mutableMapOf<String, Any>("backspace_speed_percent" to 60,
            "backspace_release_delete" to false)
        val p = prefs(values)
        val captured = readEditBehaviorPreferences(p)
        values["backspace_speed_percent"] = 120
        values["backspace_release_delete"] = true
        val refreshed = readEditBehaviorPreferences(p)
        assertFalse(captured.releaseDelete)
        assertTrue(refreshed.releaseDelete)
    }
}
