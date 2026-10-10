package tribixbite.cleverkeys

import android.content.ClipboardManager
import android.os.Build
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection

/** One visible-editor session, before its first edit. No history, coercion, logging or learning. */
class ClipboardPasteSuggestions(
    private val clipboard: ClipboardManager,
    private val show: (String?, (() -> Unit)?) -> Unit,
    private val editor: () -> Pair<InputConnection?, EditorInfo?>,
    private val paste: (String) -> Unit
) {
    private var connection: InputConnection? = null
    private var info: EditorInfo? = null
    private var active = false
    private var listening = false
    private var revision = 0L
    private val listener = ClipboardManager.OnPrimaryClipChangedListener { refresh() }

    fun start(connection: InputConnection?, info: EditorInfo) {
        stop()
        this.connection = connection
        this.info = info
        active = connection != null && !SuggestionBar.isPasswordField(info) &&
            LearningGate.fieldAllowsPersonalizedLearning(info.imeOptions)
        if (!active) return
        try {
            clipboard.addPrimaryClipChangedListener(listener)
            listening = true
        } catch (_: Exception) { /* One-shot offer can still be revalidated at tap. */ }
        refresh()
    }

    fun dismiss() {
        val wasActive = active
        active = false
        revision++
        if (wasActive) show(null, null)
    }

    fun stop() {
        if (listening) try { clipboard.removePrimaryClipChangedListener(listener) } catch (_: Exception) { }
        listening = false
        dismiss()
        connection = null
        info = null
    }

    private fun currentEditorMatches(): Boolean {
        val current = editor()
        return active && connection != null && current.first === connection && current.second === info
    }

    private fun readText(): String? = try {
        val clip = clipboard.primaryClip
        val sensitive = Build.VERSION.SDK_INT >= 24 &&
            clip?.description?.extras?.getBoolean("android.content.extra.IS_SENSITIVE", false) == true
        if (clip == null || clip.itemCount != 1 || sensitive) null
        else clip.getItemAt(0).text?.takeIf { it.length in 1..65536 && it.isNotBlank() }?.toString()
    } catch (_: Exception) { null } // Binder denial, empty clipboard or oversized transaction

    private fun refresh() {
        if (!currentEditorMatches()) { dismiss(); return }
        val offered = readText()
        val ticket = ++revision
        show(offered, offered?.let { text ->
            {
                // A queued tap cannot paste into a new editor or a changed/sensitive clip.
                if (revision == ticket && currentEditorMatches() && readText() == text) {
                    dismiss()
                    paste(text)
                } else if (revision == ticket) dismiss()
            }
        })
    }
}
