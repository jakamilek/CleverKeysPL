package tribixbite.cleverkeys

/** Captured once per gesture; settings refresh cannot change an active drag. */
data class EditBehaviorOptions(
    val tapMode: Int = Defaults.BACKSPACE_TAP_MODE,
    val holdSelect: Boolean = Defaults.BACKSPACE_HOLD_SELECT,
    val releaseDelete: Boolean = Defaults.BACKSPACE_RELEASE_DELETE,
    val punctuationRemoveSpace: Boolean = Defaults.PUNCTUATION_REMOVE_SPACE,
    val punctuationAddSpace: Boolean = Defaults.PUNCTUATION_ADD_SPACE,
    val formatSearchFields: Boolean = Defaults.FORMAT_SEARCH_FIELDS,
    val numericPeriodCaps: Boolean = Defaults.NUMERIC_PERIOD_CAPS,
    val shiftWordCase: Boolean = Defaults.SHIFT_WORD_CASE,
    val shiftWordEnd: Boolean = Defaults.SHIFT_WORD_END,
    val showCaseVariants: Boolean = Defaults.SHOW_CASE_VARIANTS,
    val exactAddFirst: Boolean = Defaults.EXACT_ADD_FIRST,
    val resetSuggestionsOnDelete: Boolean = Defaults.RESET_SUGGESTIONS_ON_DELETE
)

/** Bounds shared by settings, runtime reads and backup import. */
object EditBehaviorRanges {
    val BACKSPACE_TAP_MODE = 0..2
}
