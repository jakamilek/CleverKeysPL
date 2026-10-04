package tribixbite.cleverkeys.ui.settings.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tribixbite.cleverkeys.R
import tribixbite.cleverkeys.SettingsActivity
import tribixbite.cleverkeys.readEditBehaviorPreferences
import tribixbite.cleverkeys.ui.settings.SettingsSwitch
import tribixbite.cleverkeys.ui.settings.SettingsDropdown
import tribixbite.cleverkeys.ui.settings.saveSetting

@Composable
internal fun SettingsActivity.BackspaceEditingControls() {
    Text(stringResource(R.string.edit_backspace_header))
    Text(stringResource(R.string.edit_backspace_help))
    SettingsDropdown(
        title = stringResource(R.string.edit_backspace_tap_mode_title), description = "",
        options = listOf(stringResource(R.string.edit_tap_character),
            stringResource(R.string.edit_tap_autocorrect), stringResource(R.string.edit_tap_swipe)),
        selectedIndex = editBehavior.tapMode,
        onSelectionChange = {
            editBehavior = editBehavior.copy(tapMode = it)
            saveSetting("backspace_tap_mode", it)
        }
    )
    SettingsSwitch(
        title = stringResource(R.string.edit_backspace_hold_select_title), description = "",
        checked = editBehavior.holdSelect, enabled = true,
        onCheckedChange = {
            editBehavior = editBehavior.copy(holdSelect = it)
            saveSetting("backspace_hold_select", it)
        }
    )
    SettingsSwitch(
        title = stringResource(R.string.edit_backspace_release_delete_title), description = "",
        checked = editBehavior.releaseDelete, enabled = editBehavior.holdSelect,
        onCheckedChange = {
            editBehavior = editBehavior.copy(releaseDelete = it)
            saveSetting("backspace_release_delete", it)
        }
    )
    OutlinedButton(onClick = {
        val editor = prefs.edit()
        editor.remove("backspace_tap_mode")
        editor.remove("backspace_hold_select")
        editor.remove("backspace_release_delete")
        editor.remove("backspace_pause_enabled")
        editor.remove("backspace_pause_dp")
        editor.remove("backspace_resume_dp")
        editor.remove("backspace_speed_percent")
        editor.remove("backspace_fast_percent")
        editor.remove("backspace_accel_percent")
        editor.apply()
        editBehavior = readEditBehaviorPreferences(prefs)
    }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.extra_keys_reset)) }
}
