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
import tribixbite.cleverkeys.ui.settings.saveSetting

@Composable
internal fun SettingsActivity.EditingFormatControls() {
    Text(stringResource(R.string.edit_format_header))
    SettingsSwitch(
        title = stringResource(R.string.edit_punctuation_remove_space_title), description = "",
        checked = editBehavior.punctuationRemoveSpace, enabled = smartPunctuationEnabled,
        onCheckedChange = {
            editBehavior = editBehavior.copy(punctuationRemoveSpace = it)
            saveSetting("punctuation_remove_space", it)
        }
    )
    SettingsSwitch(
        title = stringResource(R.string.edit_punctuation_add_space_title), description = "",
        checked = editBehavior.punctuationAddSpace, enabled = smartPunctuationEnabled,
        onCheckedChange = {
            editBehavior = editBehavior.copy(punctuationAddSpace = it)
            saveSetting("punctuation_add_space", it)
        }
    )
    SettingsSwitch(
        title = stringResource(R.string.edit_format_search_fields_title), description = "",
        checked = editBehavior.formatSearchFields, enabled = true,
        onCheckedChange = {
            editBehavior = editBehavior.copy(formatSearchFields = it)
            saveSetting("format_search_fields", it)
        }
    )
    SettingsSwitch(
        title = stringResource(R.string.edit_numeric_period_caps_title), description = "",
        checked = editBehavior.numericPeriodCaps, enabled = autoCapitalizationEnabled,
        onCheckedChange = {
            editBehavior = editBehavior.copy(numericPeriodCaps = it)
            saveSetting("numeric_period_caps", it)
        }
    )
    SettingsSwitch(
        title = stringResource(R.string.edit_shift_word_case_title), description = "",
        checked = editBehavior.shiftWordCase, enabled = true,
        onCheckedChange = {
            editBehavior = editBehavior.copy(shiftWordCase = it)
            saveSetting("shift_word_case", it)
        }
    )
    SettingsSwitch(
        title = stringResource(R.string.edit_shift_word_end_title), description = "",
        checked = editBehavior.shiftWordEnd, enabled = editBehavior.shiftWordCase,
        onCheckedChange = {
            editBehavior = editBehavior.copy(shiftWordEnd = it)
            saveSetting("shift_word_end", it)
        }
    )
    Text(stringResource(R.string.edit_format_help))
    OutlinedButton(onClick = {
        val editor = prefs.edit()
        editor.remove("punctuation_remove_space")
        editor.remove("punctuation_add_space")
        editor.remove("format_search_fields")
        editor.remove("numeric_period_caps")
        editor.remove("shift_word_case")
        editor.remove("shift_word_end")
        editor.apply()
        editBehavior = readEditBehaviorPreferences(prefs)
    }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.extra_keys_reset)) }
}

@Composable
internal fun SettingsActivity.EditingSuggestionControls() {
    SettingsSwitch(
        title = stringResource(R.string.edit_show_case_variants_title), description = "",
        checked = editBehavior.showCaseVariants, enabled = true,
        onCheckedChange = {
            editBehavior = editBehavior.copy(showCaseVariants = it)
            saveSetting("show_case_variants", it)
        }
    )
    SettingsSwitch(
        title = stringResource(R.string.edit_exact_add_first_title), description = "",
        checked = editBehavior.exactAddFirst, enabled = showExactTypedWord,
        onCheckedChange = {
            editBehavior = editBehavior.copy(exactAddFirst = it)
            saveSetting("exact_add_first", it)
        }
    )
    SettingsSwitch(
        title = stringResource(R.string.edit_reset_suggestions_on_delete_title), description = "",
        checked = editBehavior.resetSuggestionsOnDelete, enabled = true,
        onCheckedChange = {
            editBehavior = editBehavior.copy(resetSuggestionsOnDelete = it)
            saveSetting("reset_suggestions_on_delete", it)
        }
    )
    OutlinedButton(onClick = {
        val editor = prefs.edit()
        editor.remove("show_case_variants")
        editor.remove("exact_add_first")
        editor.remove("reset_suggestions_on_delete")
        editor.apply()
        editBehavior = readEditBehaviorPreferences(prefs)
    }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.extra_keys_reset)) }
}
