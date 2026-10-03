package tribixbite.cleverkeys

import android.text.InputType
import android.view.inputmethod.EditorInfo

/** Automatic prose formatting is independent of prediction/cursor-sync eligibility. */
object EditorSpacingPolicy {
    fun allowsAutomaticSpacing(info: EditorInfo?): Boolean {
        info ?: return false
        if ((info.imeOptions and EditorInfo.IME_MASK_ACTION) == EditorInfo.IME_ACTION_SEARCH) {
            return false
        }
        if ((info.inputType and InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT) {
            return false
        }
        return when (info.inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> false
            else -> true
        }
    }
}
