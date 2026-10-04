package tribixbite.cleverkeys

/** Captured once per gesture; settings refresh cannot change an active drag. */
data class EditBehaviorOptions(
    val tapMode: Int = Defaults.BACKSPACE_TAP_MODE,
    val holdSelect: Boolean = Defaults.BACKSPACE_HOLD_SELECT,
    val releaseDelete: Boolean = Defaults.BACKSPACE_RELEASE_DELETE,
    val pauseEnabled: Boolean = Defaults.BACKSPACE_PAUSE_ENABLED,
    val pauseDp: Int = Defaults.BACKSPACE_PAUSE_DP,
    val resumeDp: Int = Defaults.BACKSPACE_RESUME_DP,
    val speedPercent: Int = Defaults.BACKSPACE_SPEED_PERCENT,
    val fastPercent: Int = Defaults.BACKSPACE_FAST_PERCENT,
    val accelPercent: Int = Defaults.BACKSPACE_ACCEL_PERCENT,
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
    val BACKSPACE_PAUSE_DP = 3..12
    val BACKSPACE_RESUME_DP = 12..48
    val BACKSPACE_SPEED_PERCENT = 40..150
    val BACKSPACE_FAST_PERCENT = 100..300
    val BACKSPACE_ACCEL_PERCENT = 30..80
}
