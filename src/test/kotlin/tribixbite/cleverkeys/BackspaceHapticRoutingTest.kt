package tribixbite.cleverkeys

import android.view.View
import io.mockk.*
import org.junit.Test

/** Actual dispatch, including both existing user gates; no direct Vibrator bypass. */
class BackspaceHapticRoutingTest {
    @Test fun bothWordEventsUseTheMasterAndLongPressToggle() {
        val view = mockk<View>(relaxed = true)
        val config = mockk<Config>(relaxed = true)
        config.vibrate_custom = false
        every { view.performHapticFeedback(any(), any()) } returns true
        for (event in listOf(HapticEvent.BACKSPACE_WORD_SELECT, HapticEvent.BACKSPACE_WORD_DELETE)) {
            clearMocks(view, answers = false)
            config.haptic_enabled = false; config.haptic_long_press = true
            VibratorCompat.vibrate(view, config, event)
            verify(exactly = 0) { view.performHapticFeedback(any(), any()) }
            config.haptic_enabled = true; config.haptic_long_press = false
            VibratorCompat.vibrate(view, config, event)
            verify(exactly = 0) { view.performHapticFeedback(any(), any()) }
            config.haptic_long_press = true
            VibratorCompat.vibrate(view, config, event)
            verify(exactly = 1) { view.performHapticFeedback(any(), any()) }
        }
    }
}
