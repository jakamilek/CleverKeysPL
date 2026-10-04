package tribixbite.cleverkeys.ui.settings.sections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import tribixbite.cleverkeys.EditBehaviorRanges
import tribixbite.cleverkeys.R
import tribixbite.cleverkeys.SettingsActivity
import tribixbite.cleverkeys.readEditBehaviorPreferences
import tribixbite.cleverkeys.ui.settings.SettingsSwitch
import tribixbite.cleverkeys.ui.settings.SettingsSlider
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
    SettingsSwitch(
        title = stringResource(R.string.edit_backspace_pause_enabled_title), description = "",
        checked = editBehavior.pauseEnabled, enabled = editBehavior.holdSelect,
        onCheckedChange = {
            editBehavior = editBehavior.copy(pauseEnabled = it)
            saveSetting("backspace_pause_enabled", it)
        }
    )
    SettingsSlider(
        title = stringResource(R.string.edit_backspace_pause_dp_title),
        description = "",
        value = editBehavior.pauseDp.toFloat(),
        valueRange = EditBehaviorRanges.BACKSPACE_PAUSE_DP.first.toFloat()..EditBehaviorRanges.BACKSPACE_PAUSE_DP.last.toFloat(),
        steps = 8, enabled = editBehavior.holdSelect && editBehavior.pauseEnabled,
        onValueChange = {
            val value = it.toInt().coerceIn(EditBehaviorRanges.BACKSPACE_PAUSE_DP)
            editBehavior = editBehavior.copy(pauseDp = value)
            saveSetting("backspace_pause_dp", value)
        }, displayValue = "${editBehavior.pauseDp} dp"
    )
    SettingsSlider(
        title = stringResource(R.string.edit_backspace_resume_dp_title),
        description = stringResource(R.string.edit_pause_help),
        value = editBehavior.resumeDp.toFloat(),
        valueRange = EditBehaviorRanges.BACKSPACE_RESUME_DP.first.toFloat()..EditBehaviorRanges.BACKSPACE_RESUME_DP.last.toFloat(),
        steps = 35, enabled = editBehavior.holdSelect && editBehavior.pauseEnabled,
        onValueChange = {
            val value = it.toInt().coerceIn(EditBehaviorRanges.BACKSPACE_RESUME_DP)
            editBehavior = editBehavior.copy(resumeDp = value)
            saveSetting("backspace_resume_dp", value)
        }, displayValue = "${editBehavior.resumeDp} dp"
    )
    SettingsSlider(
        title = stringResource(R.string.edit_backspace_speed_percent_title),
        description = stringResource(R.string.edit_speed_help),
        value = editBehavior.speedPercent.toFloat(),
        valueRange = EditBehaviorRanges.BACKSPACE_SPEED_PERCENT.first.toFloat()..EditBehaviorRanges.BACKSPACE_SPEED_PERCENT.last.toFloat(),
        steps = 109, enabled = editBehavior.holdSelect,
        onValueChange = {
            val value = it.toInt().coerceIn(EditBehaviorRanges.BACKSPACE_SPEED_PERCENT)
            editBehavior = editBehavior.copy(speedPercent = value)
            saveSetting("backspace_speed_percent", value)
        }, displayValue = "${editBehavior.speedPercent} %"
    )
    SettingsSlider(
        title = stringResource(R.string.edit_backspace_fast_percent_title),
        description = "",
        value = editBehavior.fastPercent.toFloat(),
        valueRange = EditBehaviorRanges.BACKSPACE_FAST_PERCENT.first.toFloat()..EditBehaviorRanges.BACKSPACE_FAST_PERCENT.last.toFloat(),
        steps = 199, enabled = editBehavior.holdSelect,
        onValueChange = {
            val value = it.toInt().coerceIn(EditBehaviorRanges.BACKSPACE_FAST_PERCENT)
            editBehavior = editBehavior.copy(fastPercent = value)
            saveSetting("backspace_fast_percent", value)
        }, displayValue = "${editBehavior.fastPercent} %"
    )
    SettingsSlider(
        title = stringResource(R.string.edit_backspace_accel_percent_title),
        description = stringResource(R.string.edit_fast_help),
        value = editBehavior.accelPercent.toFloat(),
        valueRange = EditBehaviorRanges.BACKSPACE_ACCEL_PERCENT.first.toFloat()..EditBehaviorRanges.BACKSPACE_ACCEL_PERCENT.last.toFloat(),
        steps = 49, enabled = editBehavior.holdSelect,
        onValueChange = {
            val value = it.toInt().coerceIn(EditBehaviorRanges.BACKSPACE_ACCEL_PERCENT)
            editBehavior = editBehavior.copy(accelPercent = value)
            saveSetting("backspace_accel_percent", value)
        }, displayValue = "${editBehavior.accelPercent} %"
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
